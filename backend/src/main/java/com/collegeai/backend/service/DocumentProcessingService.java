package com.collegeai.backend.service;

import com.collegeai.backend.entity.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles processing of uploaded documents.
 *
 * Downloads the PDF from Cloudinary, extracts its text,
 * and divides the extracted text into smaller chunks.
 */
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final PdfDownloadService pdfDownloadService;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final DocumentChunkingService documentChunkingService;

    /**
     * Downloads a document from Cloudinary,
     * extracts its text, and creates text chunks.
     *
     * @param document uploaded document metadata
     * @return list of extracted text chunks
     */
    public List<String> processDocument(Document document) throws Exception {

        // 1. Download the PDF from Cloudinary.
        byte[] pdfBytes =
                pdfDownloadService.downloadPdf(
                        document.getSecureUrl()
                );

        // 2. Extract readable text from the PDF.
        String extractedText =
                pdfTextExtractionService.extractText(pdfBytes);

        // 3. Split the extracted text into smaller chunks.
        return documentChunkingService.createChunks(extractedText);
    }
}