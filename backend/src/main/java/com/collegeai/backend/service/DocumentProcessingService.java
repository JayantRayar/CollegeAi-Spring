 package com.collegeai.backend.service;
import com.collegeai.backend.dto.DocumentChunk;
import com.collegeai.backend.entity.Document;
import com.collegeai.backend.entity.DocumentStatus;
import com.collegeai.backend.exception.DocumentNotFoundException;
import com.collegeai.backend.exception.DocumentProcessingException;
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
    private final CloudinaryService cloudinaryService;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            PdfDownloadService pdfDownloadService,
            PdfTextExtractionService pdfTextExtractionService,
            DocumentChunkingService documentChunkingService,
            DocumentVectorStoreService documentVectorStoreService,
            CloudinaryService cloudinaryService
    ) {
        this.documentRepository = documentRepository;
        this.pdfDownloadService = pdfDownloadService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.documentChunkingService = documentChunkingService;
        this.documentVectorStoreService = documentVectorStoreService;
        this.cloudinaryService = cloudinaryService;
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
    public List<DocumentChunk> processDocument(Long documentId) {

        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Document not found with id: "
                                                + documentId
                                )
                        );

        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        try {

            byte[] pdfBytes =
                    pdfDownloadService.downloadPdf(
                            document.getSecureUrl()
                    );

            List<PdfTextExtractionService.ExtractedPage> pages =
                    pdfTextExtractionService.extractPages(pdfBytes);

            List<DocumentChunk> chunks =
                    new ArrayList<>();

            AtomicInteger chunkCounter =
                    new AtomicInteger(1);

            String previousPageText = null;

            String documentName =
                    document.getOriginalFilename();

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

                previousPageText = pageText;
            }

            documentVectorStoreService.deleteDocumentChunks(
                    documentId
            );

            documentVectorStoreService.storeChunks(
                    chunks
            );

            document.setStatus(DocumentStatus.PROCESSED);
            documentRepository.save(document);

            return chunks;

        } catch (Exception e) {

            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);

            throw new DocumentProcessingException(
                    "Failed to process document.",
                    e
            );
        }
    }

    /**
     * Deletes a document and all associated data.
     *
     * Deletion flow:
     *
     * PostgreSQL
     *     ↓
     * Verify document exists
     *     ↓
     * ChromaDB
     *     ↓
     * Delete vector data
     *     ↓
     * Cloudinary
     *     ↓
     * Delete original PDF
     *     ↓
     * PostgreSQL
     *     ↓
     * Delete document metadata
     */
    public void deleteDocument(Long documentId) {

        // Make sure the document exists before deleting anything.
        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Document not found with id: "
                                                + documentId
                                )
                        );

        // Remove all ChromaDB vectors belonging
        // to this document.
        documentVectorStoreService.deleteDocumentChunks(
                documentId
        );

        // Remove the original PDF from Cloudinary.
        cloudinaryService.deleteFile(
                document.getPublicId()
        );

        // Remove the document metadata from PostgreSQL.
        documentRepository.delete(document);
    }
}

