
package com.collegeai.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingScheduler {

    private final DocumentProcessingJobWorkerService workerService;
    private final DocumentProcessingService documentProcessingService;

    // Periodically claim and process one pending document job.
    @Scheduled(fixedDelayString = "${app.document-processing.worker-delay:5000}")
    public void processNextJob() {

        Optional<ClaimedDocumentJob> optionalJob =
                workerService.claimNextJob();

        if (optionalJob.isEmpty()) {
            return;
        }

        ClaimedDocumentJob job = optionalJob.get();

        log.info(
                "Starting document processing jobId={}, documentId={}",
                job.jobId(),
                job.documentId()
        );

        try {

            // Execute the existing PDF processing logic.
            documentProcessingService.processDocument(job.documentId());

        } catch (Exception exception) {

            log.error(
                    "Document processing failed. jobId={}, documentId={}",
                    job.jobId(),
                    job.documentId(),
                    exception
            );

            // Schedule retry or mark the job as permanently failed.
            workerService.markFailed(job.jobId(), exception);

            return;
        }

        // Keep completion handling outside the processing catch block.
        workerService.markCompleted(job.jobId());

        log.info(
                "Document processing completed successfully. jobId={}",
                job.jobId()
        );
    }

    // Check periodically for jobs abandoned by crashed workers.
    @Scheduled(fixedDelayString = "${app.document-processing.recovery-delay:60000}")
    public void recoverStuckJobs() {

        try {

            boolean recovered = workerService.recoverOneStuckJob();

            if (recovered) {
                log.warn("Recovered one stuck document processing job.");
            }

        } catch (Exception exception) {

            log.error("Failed to recover a stuck document processing job.", exception);
        }
    }
}
