package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import com.collegeai.backend.entity.Document;
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

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            PdfDownloadService pdfDownloadService,
            PdfTextExtractionService pdfTextExtractionService,
            DocumentChunkingService documentChunkingService
    ) {
        this.documentRepository = documentRepository;
        this.pdfDownloadService = pdfDownloadService;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.documentChunkingService = documentChunkingService;
    }

    public List<DocumentChunk> processDocument(Long documentId) throws Exception {

        // Find document metadata from PostgreSQL
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Document not found with id: " + documentId
                        )
                );

        // Download the PDF from Cloudinary
        byte[] pdfBytes =
                pdfDownloadService.downloadPdf(document.getSecureUrl());

        // Extract text page-by-page
        List<String> pages =
                pdfTextExtractionService.extractPages(pdfBytes);

        // Store all chunks from all pages
        List<DocumentChunk> chunks = new ArrayList<>();

        // Keep one counter for the entire document
        AtomicInteger chunkCounter = new AtomicInteger(1);

        // Process each PDF page separately
        for (int pageNumber = 1;
             pageNumber <= pages.size();
             pageNumber++) {

            String pageText = pages.get(pageNumber - 1);

            chunks.addAll(
                    documentChunkingService.createChunks(
                            pageText,
                            pageNumber,
                            chunkCounter
                    )
            );
        }

        return chunks;
    }
}