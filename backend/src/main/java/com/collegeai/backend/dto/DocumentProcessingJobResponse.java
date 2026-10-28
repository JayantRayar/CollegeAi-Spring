
package com.collegeai.backend.dto;

import com.collegeai.backend.entity.DocumentProcessingJob;
import com.collegeai.backend.entity.DocumentProcessingJobStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DocumentProcessingJobResponse {

    private Long jobId;

    private Long documentId;

    private DocumentProcessingJobStatus status;

    private int attemptCount;

    private int maxAttempts;

    private LocalDateTime createdAt;

    public static DocumentProcessingJobResponse from(
            DocumentProcessingJob job) {

        return new DocumentProcessingJobResponse(
                job.getId(),
                job.getDocument().getId(),
                job.getStatus(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getCreatedAt()
        );
    }
}
