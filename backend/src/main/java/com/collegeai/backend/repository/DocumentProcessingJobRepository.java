package com.collegeai.backend.repository;

import com.collegeai.backend.entity.DocumentProcessingJob;
import com.collegeai.backend.entity.DocumentProcessingJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface DocumentProcessingJobRepository
        extends JpaRepository<DocumentProcessingJob, Long> {

    boolean existsByDocument_IdAndStatusIn(
            Long documentId,
            Collection<DocumentProcessingJobStatus> statuses
    );

    // Find the oldest eligible pending job and lock its database row.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentProcessingJob>
    findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            DocumentProcessingJobStatus status,
            LocalDateTime currentTime
    );
    // Find and lock a processing job whose worker lock has expired.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DocumentProcessingJob>
    findFirstByStatusAndLockedAtLessThanEqualOrderByLockedAtAsc(
            DocumentProcessingJobStatus status,
            LocalDateTime lockedBefore
    );
}