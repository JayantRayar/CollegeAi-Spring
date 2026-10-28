package com.collegeai.backend.service;

import com.collegeai.backend.entity.DocumentProcessingJob;
import com.collegeai.backend.entity.DocumentProcessingJobStatus;
import com.collegeai.backend.repository.DocumentProcessingJobRepository;
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
    public Optional<Long> claimNextJob() {

        // Find and lock the oldest eligible pending job.
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

        // Save the updated job.
        jobRepository.save(job);

        // Return only the job ID, not the JPA entity.
        return Optional.of(job.getId());
    }
}