package com.collegeai.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfTextExtractionService {

    public List<String> extractPages(byte[] pdfBytes) throws IOException {

        try (var document = Loader.loadPDF(pdfBytes)) {

            PDFTextStripper textStripper = new PDFTextStripper();

            List<String> pages = new ArrayList<>();

            for (int pageNumber = 1;
                 pageNumber <= document.getNumberOfPages();
                 pageNumber++) {

                textStripper.setStartPage(pageNumber);
                textStripper.setEndPage(pageNumber);

                String pageText = textStripper.getText(document);

                if (pageText != null && !pageText.isBlank()) {
                    pages.add(pageText);
                }
            }

            return pages;
        }
    }
}