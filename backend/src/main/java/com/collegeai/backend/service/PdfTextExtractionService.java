package com.collegeai.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * PDF text extraction service.
 *
 * Extracts readable text from PDF documents using Apache PDFBox.
 */
@Service
public class PdfTextExtractionService {

    /**
     * Extracts text from a PDF.
     *
     * @param pdfBytes PDF file contents
     * @return extracted text
     */
    public String extractText(byte[] pdfBytes) throws IOException {

        try (var document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper textStripper = new PDFTextStripper();

            return textStripper.getText(document);
        }
    }
}