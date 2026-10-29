
package com.collegeai.backend.repository;

import com.collegeai.backend.entity.DocumentIndexVersion;
import com.collegeai.backend.entity.DocumentIndexVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentIndexVersionRepository
        extends JpaRepository<DocumentIndexVersion, Long> {

    // Find all index versions currently active for chatbot retrieval.
    List<DocumentIndexVersion> findAllByStatus(
            DocumentIndexVersionStatus status
    );

    // Find all active versions belonging to a specific document.
    List<DocumentIndexVersion> findAllByDocument_IdAndStatus(
            Long documentId,
            DocumentIndexVersionStatus status
    );

    // Find the latest version created for a document.
    Optional<DocumentIndexVersion>
    findTopByDocument_IdOrderByVersionNumberDesc(Long documentId);

    // Find the currently active index version for a document.
    Optional<DocumentIndexVersion>
    findByDocument_IdAndStatus(
            Long documentId,
            DocumentIndexVersionStatus status
    );

    // Delete every index version associated with a document.
    void deleteAllByDocument_Id(Long documentId);
}
