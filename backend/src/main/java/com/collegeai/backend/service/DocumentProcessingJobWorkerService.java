
package com.collegeai.backend.service;

import com.collegeai.backend.entity.DocumentProcessingJob;
import com.collegeai.backend.entity.DocumentProcessingJobStatus;
import com.collegeai.backend.repository.DocumentProcessingJobRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentProcessingJobWorkerService {

    private final DocumentProcessingJobRepository jobRepository;

    // Claims one pending job so another worker cannot claim it.
    @Transactional
    public Optional<ClaimedDocumentJob> claimNextJob() {

        Optional<DocumentProcessingJob> optionalJob =
                jobRepository
                        .findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                                DocumentProcessingJobStatus.PENDING,
                                LocalDateTime.now()
                        );

        if (optionalJob.isEmpty()) {
            return Optional.empty();
        }

        DocumentProcessingJob job = optionalJob.get();

        // Mark the job as currently being processed.
        job.setStatus(DocumentProcessingJobStatus.PROCESSING);

        // Record when this worker claimed the job.
        job.setLockedAt(LocalDateTime.now());

        // Count this processing attempt.
        job.setAttemptCount(job.getAttemptCount() + 1);

        jobRepository.save(job);

        // Return IDs instead of exposing a lazy JPA entity.
        return Optional.of(
                new ClaimedDocumentJob(
                        job.getId(),
                        job.getDocument().getId()
                )
        );
    }

    // Mark a successfully processed job as completed.
    @Transactional
    public void markCompleted(Long jobId) {

        DocumentProcessingJob job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Processing job not found with id: " + jobId
                        )
                );

        job.setStatus(DocumentProcessingJobStatus.COMPLETED);
        job.setLockedAt(null);
        job.setLastError(null);

        jobRepository.save(job);
    }

    // Handle a failed attempt and schedule a retry if attempts remain.
    @Transactional
    public void markFailed(Long jobId, Exception exception) {

        DocumentProcessingJob job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Processing job not found with id: " + jobId
                        )
                );

        String errorMessage = exception.getMessage();

        if (errorMessage == null || errorMessage.isBlank()) {
            errorMessage = exception.getClass().getSimpleName();
        }

        // Keep the error within the database column limit.
        if (errorMessage.length() > 2000) {
            errorMessage = errorMessage.substring(0, 2000);
        }

        job.setLastError(errorMessage);
        job.setLockedAt(null);

        if (job.getAttemptCount() < job.getMaxAttempts()) {

            // Exponential retry delay: 1, 2, 4, 8, 16 minutes (maximum 30).
            int exponent = Math.max(0,
                    Math.min(job.getAttemptCount() - 1, 5));

            long retryDelayMinutes = Math.min(
                    1L << exponent,
                    30L
            );

            job.setNextAttemptAt(
                    LocalDateTime.now().plusMinutes(retryDelayMinutes)
            );

            job.setStatus(DocumentProcessingJobStatus.PENDING);

        } else {

            job.setStatus(DocumentProcessingJobStatus.FAILED);
        }

        jobRepository.save(job);
    }

    // Recover one job abandoned by a crashed application or worker.
    @Transactional
    public boolean recoverOneStuckJob() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockExpiryTime = now.minusMinutes(10);

        Optional<DocumentProcessingJob> optionalJob =
                jobRepository
                        .findFirstByStatusAndLockedAtLessThanEqualOrderByLockedAtAsc(
                                DocumentProcessingJobStatus.PROCESSING,
                                lockExpiryTime
                        );

        if (optionalJob.isEmpty()) {
            return false;
        }

        DocumentProcessingJob job = optionalJob.get();

        job.setLastError(
                "Processing lock expired. Job recovered after application interruption."
        );

        job.setLockedAt(null);

        if (job.getAttemptCount() < job.getMaxAttempts()) {

            job.setStatus(DocumentProcessingJobStatus.PENDING);
            job.setNextAttemptAt(now);

        } else {

            job.setStatus(DocumentProcessingJobStatus.FAILED);
        }

        jobRepository.save(job);

        return true;
    }
}
