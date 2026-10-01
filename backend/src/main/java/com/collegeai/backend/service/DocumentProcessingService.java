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

    /**
     * Processes a document and stores its chunks
     * in the configured vector store.
     *
     * @param documentId PostgreSQL document ID
     * @return list of generated document chunks
     */
    public List<DocumentChunk> processDocument(Long documentId)
            throws Exception {

        // 1. Find document metadata from PostgreSQL.
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found with id: " + documentId
                        )
                );

        // 2. Download the PDF from Cloudinary.
        byte[] pdfBytes =
                pdfDownloadService.downloadPdf(
                        document.getSecureUrl()
                );

        // 3. Extract text page-by-page.
        List<String> pages =
                pdfTextExtractionService.extractPages(pdfBytes);

        // 4. Create chunks from all pages.
        List<DocumentChunk> chunks = new ArrayList<>();

        // Keep one counter for the entire document.
        AtomicInteger chunkCounter =
                new AtomicInteger(1);

        // Process each page separately.
        for (int pageNumber = 1;
             pageNumber <= pages.size();
             pageNumber++) {

            String pageText =
                    pages.get(pageNumber - 1);

            chunks.addAll(
                    documentChunkingService.createChunks(
                            pageText,
                            documentId,
                            pageNumber,
                            chunkCounter
                    )
            );
        }

        // 5. Generate embeddings and store chunks in ChromaDB.
        documentVectorStoreService.storeChunks(chunks);

        // 6. Return chunks so we can verify the processing result.
        return chunks;
    }
}