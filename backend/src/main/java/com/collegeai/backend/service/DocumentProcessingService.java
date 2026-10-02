package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import com.collegeai.backend.entity.Document;
import com.collegeai.backend.entity.DocumentStatus;
import com.collegeai.backend.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the complete document processing pipeline.
 *
 * PDF:
 * Cloudinary
 *     ↓
 * PDF download
 *     ↓
 * PDF text extraction
 *     ↓
 * Page-based text
 *     ↓
 * Context-aware chunking
 *     ↓
 * Remove old vectors
 *     ↓
 * Store fresh vectors
 *
 * This service also manages the document's
 * processing lifecycle status.
 */
@Service
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final PdfDownloadService pdfDownloadService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentVectorStoreService documentVectorStoreService;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            PdfDownloadService pdfDownloadService,
            PdfTextExtractionService pdfTextExtractionService,
            DocumentChunkingService documentChunkingService,
            DocumentVectorStoreService documentVectorStoreService
    ) {
        this.documentRepository = documentRepository;
        this.pdfDownloadService = pdfDownloadService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.documentChunkingService = documentChunkingService;
        this.documentVectorStoreService = documentVectorStoreService;
    }

    /**
     * Downloads a PDF, extracts its text, creates chunks,
     * removes old vectors, and stores newly generated vectors.
     *
     * Processing lifecycle:
     *
     * UPLOADED
     *     ↓
     * PROCESSING
     *     ↓
     * PROCESSED
     *
     * If anything fails:
     *
     * PROCESSING
     *     ↓
     * FAILED
     */
    public List<DocumentChunk> processDocument(Long documentId)
            throws Exception {

        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found with id: "
                                                + documentId
                                )
                        );

        /*
         * Mark the document as currently being processed.
         */
        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        try {

            // Download the original PDF from Cloudinary.
            byte[] pdfBytes =
                    pdfDownloadService.downloadPdf(
                            document.getSecureUrl()
                    );

            // Extract text page by page while preserving
            // the original PDF page numbers.
            List<PdfTextExtractionService.ExtractedPage> pages =
                    pdfTextExtractionService.extractPages(pdfBytes);

            List<DocumentChunk> chunks =
                    new ArrayList<>();

            AtomicInteger chunkCounter =
                    new AtomicInteger(1);

            String previousPageText = null;

            String documentName =
                    document.getOriginalFilename();

            // Process every extracted page.
            for (PdfTextExtractionService.ExtractedPage page : pages) {

                int pageNumber =
                        page.getPageNumber();

                String pageText =
                        page.getText();

                chunks.addAll(
                        documentChunkingService.createChunks(
                                pageText,
                                previousPageText,
                                documentId,
                                documentName,
                                pageNumber,
                                chunkCounter
                        )
                );

                // Keep the current page so that its ending
                // can be used as context for the next page.
                previousPageText = pageText;
            }

            /*
             * Remove previously indexed vectors belonging
             * to this document before storing the new ones.
             *
             * This prevents duplicate/stale vectors when
             * the same document is processed again.
             */
            documentVectorStoreService.deleteDocumentChunks(documentId);

            // Store the freshly generated chunks and embeddings.
            documentVectorStoreService.storeChunks(chunks);

            /*
             * Processing and vector indexing completed successfully.
             */
            document.setStatus(DocumentStatus.PROCESSED);
            documentRepository.save(document);

            return chunks;

        } catch (Exception e) {

            /*
             * Something went wrong during processing.
             * Mark the document as FAILED before passing
             * the original exception back to the controller.
             */
            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);

            throw e;
        }
    }

    /**
     * Deletes a document and all of its vector data.
     *
     * Vector data is removed from ChromaDB first.
     * Then the document metadata is removed from PostgreSQL.
     */
    public void deleteDocument(Long documentId) {

        // Make sure the document exists before deleting anything.
        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found with id: "
                                                + documentId
                                )
                        );

        // Remove all ChromaDB vectors belonging to this document.
        documentVectorStoreService.deleteDocumentChunks(documentId);

        // Remove the document metadata from PostgreSQL.
        documentRepository.delete(document);
    }
}