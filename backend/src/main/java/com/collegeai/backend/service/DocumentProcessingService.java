
package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import com.collegeai.backend.entity.Document;
import com.collegeai.backend.entity.DocumentIndexVersion;
import com.collegeai.backend.entity.DocumentStatus;
import com.collegeai.backend.exception.DocumentNotFoundException;
import com.collegeai.backend.exception.DocumentProcessingException;
import com.collegeai.backend.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final PdfDownloadService pdfDownloadService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentVectorStoreService documentVectorStoreService;
    private final CloudinaryService cloudinaryService;
    private final DocumentIndexVersionService documentIndexVersionService;
    private final ChromaIndexVerificationService chromaIndexVerificationService;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            PdfDownloadService pdfDownloadService,
            PdfTextExtractionService pdfTextExtractionService,
            DocumentChunkingService documentChunkingService,
            DocumentVectorStoreService documentVectorStoreService,
            CloudinaryService cloudinaryService,
            DocumentIndexVersionService documentIndexVersionService,
            ChromaIndexVerificationService chromaIndexVerificationService
    ) {
        this.documentRepository = documentRepository;
        this.pdfDownloadService = pdfDownloadService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.documentChunkingService = documentChunkingService;
        this.documentVectorStoreService = documentVectorStoreService;
        this.cloudinaryService = cloudinaryService;
        this.documentIndexVersionService = documentIndexVersionService;
        this.chromaIndexVerificationService = chromaIndexVerificationService;
    }

    /**
     * Downloads a PDF, extracts its text, creates version-specific chunks,
     * stores them, verifies them, and activates the new index version.
     */
    public List<DocumentChunk> processDocument(Long documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new DocumentNotFoundException(
                                "Document not found with id: " + documentId
                        )
                );

        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        Long indexVersionId = null;

        try {

            // Create a new staging index version.
            DocumentIndexVersion indexVersion =
                    documentIndexVersionService.createStagingVersion(documentId);

            indexVersionId = indexVersion.getId();

            // Download the PDF.
            byte[] pdfBytes =
                    pdfDownloadService.downloadPdf(document.getSecureUrl());

            // Extract text page by page.
            List<PdfTextExtractionService.ExtractedPage> pages =
                    pdfTextExtractionService.extractPages(pdfBytes);

            List<DocumentChunk> chunks = new ArrayList<>();
            AtomicInteger chunkCounter = new AtomicInteger(1);

            String previousPageText = null;
            String documentName = document.getOriginalFilename();

            // Create chunks associated with this index version.
            for (PdfTextExtractionService.ExtractedPage page : pages) {

                int pageNumber = page.getPageNumber();
                String pageText = page.getText();

                chunks.addAll(
                        documentChunkingService.createChunks(
                                pageText,
                                previousPageText,
                                documentId,
                                indexVersionId,
                                documentName,
                                pageNumber,
                                chunkCounter
                        )
                );

                previousPageText = pageText;
            }

            // Prevent an empty index from becoming active.
            if (chunks.isEmpty()) {
                throw new IllegalStateException(
                        "PDF processing produced no text chunks."
                );
            }

            // Store the new version's chunks in ChromaDB.
            documentVectorStoreService.storeChunks(chunks);

            // Verify that every expected chunk was stored successfully.
            chromaIndexVerificationService.verifyChunks(
                    indexVersionId,
                    chunks
            );

            // Update document status before activation.
            document.setStatus(DocumentStatus.PROCESSED);
            documentRepository.save(document);

            // Activate only after successful storage and verification.
            documentIndexVersionService.activateVersion(indexVersionId);

            return chunks;

        } catch (Exception e) {

            if (indexVersionId != null) {

                // Mark the staging version as failed.
                try {
                    documentIndexVersionService.markVersionFailed(
                            indexVersionId
                    );
                } catch (Exception cleanupException) {
                    e.addSuppressed(cleanupException);
                }

                // Delete vectors only if the version is still safe to clean up.
                try {
                    if (documentIndexVersionService
                            .isSafeToDeleteVersionChunks(indexVersionId)) {

                        documentVectorStoreService.deleteVersionChunks(
                                indexVersionId
                        );
                    }
                } catch (Exception cleanupException) {
                    e.addSuppressed(cleanupException);
                }
            }

            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);

            throw new DocumentProcessingException(
                    "Failed to process document.",
                    e
            );
        }
    }

    /**
     * Deletes a document and its associated data.
     * Version-aware deletion will be implemented separately.
     */
    public void deleteDocument(Long documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new DocumentNotFoundException(
                                "Document not found with id: " + documentId
                        )
                );

        documentVectorStoreService.deleteDocumentChunks(documentId);

        cloudinaryService.deleteFile(document.getPublicId());

        documentRepository.delete(document);
    }
}
