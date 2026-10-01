package com.collegeai.backend.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts text from a PDF while preserving
 * the original PDF page number.
 */
@Service
public class PdfTextExtractionService {

    /**
     * Extracts text from every non-empty PDF page.
     *
     * The original page number is preserved so that
     * later RAG citations point to the correct PDF page.
     */
    public List<ExtractedPage> extractPages(byte[] pdfBytes)
            throws IOException {

        try (var document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper textStripper =
                    new PDFTextStripper();

            List<ExtractedPage> pages =
                    new ArrayList<>();

            for (int pageNumber = 1;
                 pageNumber <= document.getNumberOfPages();
                 pageNumber++) {

                textStripper.setStartPage(pageNumber);
                textStripper.setEndPage(pageNumber);

                String pageText =
                        textStripper.getText(document);

                if (pageText != null && !pageText.isBlank()) {

                    pages.add(
                            new ExtractedPage(
                                    pageNumber,
                                    pageText
                            )
                    );
                }
            }

            return pages;
        }
    }

    /**
     * Represents text extracted from one PDF page.
     *
     * pageNumber = actual page number in the PDF.
     * text = extracted text from that page.
     */
    @Getter
    @AllArgsConstructor
    public static class ExtractedPage {

        private final int pageNumber;

        private final String text;
    }
}