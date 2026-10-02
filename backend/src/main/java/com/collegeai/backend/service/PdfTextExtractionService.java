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
 *
 * Text is extracted according to its visual position
 * on the page so that structured content such as
 * tables has a better chance of maintaining its
 * original reading order.
 */
@Service
public class PdfTextExtractionService {

    /**
     * Extracts text from every non-empty PDF page.
     *
     * @param pdfBytes PDF file contents
     * @return extracted text grouped by original PDF page
     * @throws IOException if the PDF cannot be read
     */
    public List<ExtractedPage> extractPages(byte[] pdfBytes)
            throws IOException {

        try (var document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper textStripper =
                    new PDFTextStripper();

            /*
             * Extract text according to its position
             * on the PDF page.
             *
             * This is particularly useful for documents
             * containing tables, columns, and structured
             * layouts.
             */
            textStripper.setSortByPosition(true);

            /*
             * Keep words separated when PDF text
             * extraction encounters separate text blocks.
             */
            textStripper.setWordSeparator(" ");

            /*
             * Keep each extracted line separated.
             */
            textStripper.setLineSeparator("\n");

            List<ExtractedPage> pages =
                    new ArrayList<>();

            /*
             * Process every original PDF page.
             *
             * We deliberately keep the original page number
             * even when a page contains no extractable text.
             */
            for (int pageNumber = 1;
                 pageNumber <= document.getNumberOfPages();
                 pageNumber++) {

                textStripper.setStartPage(pageNumber);
                textStripper.setEndPage(pageNumber);

                String pageText =
                        textStripper.getText(document);

                /*
                 * Ignore completely empty pages.
                 */
                if (pageText != null
                        && !pageText.isBlank()) {

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
     * Represents text extracted from one original
     * PDF page.
     */
    @Getter
    @AllArgsConstructor
    public static class ExtractedPage {

        private final int pageNumber;
        private final String text;
    }
}