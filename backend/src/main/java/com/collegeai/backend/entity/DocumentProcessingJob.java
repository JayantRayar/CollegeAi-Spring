
package com.collegeai.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_processing_jobs")
@Getter
@Setter
@NoArgsConstructor
public class DocumentProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The document this job is responsible for processing.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    // Current state of this processing job.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentProcessingJobStatus status =
            DocumentProcessingJobStatus.PENDING;

    // Number of processing attempts made so far.
    @Column(nullable = false)
    private int attemptCount = 0;

    // Maximum number of attempts allowed.
    @Column(nullable = false)
    private int maxAttempts = 3;

    // Earliest time this job can be retried.
    private LocalDateTime nextAttemptAt;

    // Time when a worker claimed this job.
    private LocalDateTime lockedAt;

    // Stores the latest failure reason.
    @Column(length = 2000)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
