package com.collegeai.backend.service;

import com.collegeai.backend.entity.Document;
import com.collegeai.backend.entity.DocumentIndexVersion;
import com.collegeai.backend.entity.DocumentIndexVersionStatus;
import com.collegeai.backend.exception.DocumentNotFoundException;
import com.collegeai.backend.repository.DocumentIndexVersionRepository;
import com.collegeai.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class DocumentIndexVersionService {

    private final DocumentRepository documentRepository;
    private final DocumentIndexVersionRepository versionRepository;


    @Transactional
    public DocumentIndexVersion createStagingVersion(Long documentId) {

        // Lock the document before calculating the next version number.
        Document document = documentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() ->
                        new DocumentNotFoundException(
                                "Document not found with id: " + documentId
                        )
                );

        // Find the latest version number after acquiring the lock.
        int nextVersionNumber = versionRepository
                .findTopByDocument_IdOrderByVersionNumberDesc(documentId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);

        // Create the new staging version.
        DocumentIndexVersion version = new DocumentIndexVersion();

        version.setDocument(document);
        version.setVersionNumber(nextVersionNumber);
        version.setStatus(DocumentIndexVersionStatus.STAGING);

        return versionRepository.save(version);
    }

    /**
     * Returns IDs of all active index versions.
     * Only these versions should be eligible for chatbot retrieval.
     */
    public List<Long> getActiveVersionIds() {

        return versionRepository
                .findAllByStatus(DocumentIndexVersionStatus.ACTIVE)
                .stream()
                .map(DocumentIndexVersion::getId)
                .toList();
    }

    @Transactional
    public void activateVersion(Long indexVersionId) {

        // Find the requested version.
        DocumentIndexVersion version = versionRepository.findById(indexVersionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Index version not found: " + indexVersionId
                        )
                );

        Long documentId = version.getDocument().getId();

        // Lock the parent document row.
        // Concurrent activations for this document must wait.
        documentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() ->
                        new DocumentNotFoundException(
                                "Document not found with id: " + documentId
                        )
                );

        // Reload the version after acquiring the lock.
        version = versionRepository.findById(indexVersionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Index version not found: " + indexVersionId
                        )
                );

        // Only a STAGING version can be activated.
        if (version.getStatus() != DocumentIndexVersionStatus.STAGING) {
            throw new IllegalStateException(
                    "Only STAGING versions can be activated"
            );
        }

        // Find currently active versions for this document.
        List<DocumentIndexVersion> activeVersions =
                versionRepository.findAllByDocument_IdAndStatus(
                        documentId,
                        DocumentIndexVersionStatus.ACTIVE
                );

        // Retire previous active versions.
        for (DocumentIndexVersion activeVersion : activeVersions) {
            activeVersion.setStatus(DocumentIndexVersionStatus.RETIRED);
        }

        // Activate the new version.
        version.setStatus(DocumentIndexVersionStatus.ACTIVE);
        version.setActivatedAt(LocalDateTime.now());

        versionRepository.saveAll(activeVersions);
        versionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public boolean isSafeToDeleteVersionChunks(Long indexVersionId) {

        DocumentIndexVersion version = versionRepository.findById(indexVersionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Index version not found: " + indexVersionId
                        )
                );

        // Only STAGING and FAILED versions are eligible for cleanup.
        return version.getStatus() == DocumentIndexVersionStatus.STAGING
                || version.getStatus() == DocumentIndexVersionStatus.FAILED;
    }

    @Transactional
    public void markVersionFailed(Long indexVersionId) {

        DocumentIndexVersion version = versionRepository
                .findById(indexVersionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Index version not found: " + indexVersionId
                ));

        if (version.getStatus() == DocumentIndexVersionStatus.STAGING) {
            version.setStatus(DocumentIndexVersionStatus.FAILED);
            versionRepository.save(version);
        }
    }
}