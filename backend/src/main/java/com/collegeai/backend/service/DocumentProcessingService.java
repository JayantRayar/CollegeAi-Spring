package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import com.collegeai.backend.entity.Document;
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
 * PDF extraction
 *     ↓
 * Page-based text
 *     ↓
 * Text chunks
 *     ↓
 * Vector store
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

    public List<DocumentChunk> processDocument(Long documentId)
            throws Exception {

        // Find document metadata from PostgreSQL.
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found with id: " + documentId
                        )
                );

        // Download the original PDF from Cloudinary.
        byte[] pdfBytes =
                pdfDownloadService.downloadPdf(
                        document.getSecureUrl()
                );

        // Extract text while preserving actual PDF page numbers.
        List<PdfTextExtractionService.ExtractedPage> pages =
                pdfTextExtractionService.extractPages(pdfBytes);

        List<DocumentChunk> chunks = new ArrayList<>();

        // Gives every chunk a unique number within this processing run.
        AtomicInteger chunkCounter =
                new AtomicInteger(1);

        for (PdfTextExtractionService.ExtractedPage page : pages) {

            int pageNumber = page.getPageNumber();

            String pageText = page.getText();

            chunks.addAll(
                    documentChunkingService.createChunks(
                            pageText,
                            documentId,
                            pageNumber,
                            chunkCounter
                    )
            );
        }

        // Store chunks and their embeddings in ChromaDB.
        documentVectorStoreService.storeChunks(chunks);

        return chunks;
    }
}