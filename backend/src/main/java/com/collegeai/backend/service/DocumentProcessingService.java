package com.collegeai.backend.service;

import com.collegeai.backend.entity.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Handles processing of uploaded documents.
 *
 * Downloads the PDF from Cloudinary and extracts
 * its readable text.
 */
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final PdfDownloadService pdfDownloadService;
    private final PdfTextExtractionService pdfTextExtractionService;

    /**
     * Downloads a document from Cloudinary and extracts its text.
     *
     * @param document uploaded document metadata
     * @return extracted PDF text
     */
    public String processDocument(Document document) throws Exception {

        // Download the PDF from Cloudinary.
        byte[] pdfBytes =
                pdfDownloadService.downloadPdf(document.getSecureUrl());

        // Extract readable text from the PDF.
        return pdfTextExtractionService.extractText(pdfBytes);
    }
}