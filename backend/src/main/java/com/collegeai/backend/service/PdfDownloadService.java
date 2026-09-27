package com.collegeai.backend.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;

/**
 * Downloads PDF files from their Cloudinary HTTPS URL.
 */
@Service
public class PdfDownloadService {

    /**
     * Downloads a PDF from the given URL.
     *
     * @param fileUrl HTTPS URL of the PDF
     * @return PDF contents as byte array
     */
    public byte[] downloadPdf(String fileUrl) throws IOException {

        URI uri = URI.create(fileUrl);

        URLConnection connection = uri.toURL().openConnection();

        try (InputStream inputStream = connection.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }
}