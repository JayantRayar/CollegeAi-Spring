
package com.collegeai.backend.repository;

import com.collegeai.backend.entity.Document;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import com.collegeai.backend.entity.DocumentStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;




public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.id = :documentId")
    Optional<Document> findByIdForUpdate(
            @Param("documentId") Long documentId
    );
    @Modifying
    @Query("""
       UPDATE Document d
       SET d.status = :newStatus
       WHERE d.id = :documentId
       AND d.status <> :processingStatus
       """)
    int claimDocumentForProcessing(
            @Param("documentId") Long documentId,
            @Param("newStatus") DocumentStatus newStatus,
            @Param("processingStatus") DocumentStatus processingStatus
    );
}
