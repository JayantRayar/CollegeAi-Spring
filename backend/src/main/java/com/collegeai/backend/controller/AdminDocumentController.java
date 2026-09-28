package com.collegeai.backend.controller;

import com.cloudinary.Cloudinary;
import com.collegeai.backend.entity.Document;
import com.collegeai.backend.repository.DocumentRepository;
import com.collegeai.backend.service.CloudinaryService;
import com.collegeai.backend.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Admin document module.
 *
 * Handles PDF uploads and stores document metadata.
 */
@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final CloudinaryService cloudinaryService;
    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;
    /**
     * Uploads a PDF to Cloudinary and stores its metadata in PostgreSQL.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please select a file to upload.");
        }

        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body("Only PDF files are allowed.");
        }

        try {

            // 1. Upload PDF to Cloudinary.
            Map uploadResult = cloudinaryService.uploadFile(file);

            // 2. Create document metadata object.
            Document document = new Document();

            document.setOriginalFilename(file.getOriginalFilename());
            document.setPublicId((String) uploadResult.get("public_id"));
            document.setSecureUrl((String) uploadResult.get("secure_url"));
            document.setResourceType((String) uploadResult.get("resource_type"));
            document.setFileSize(file.getSize());

            // 3. Save metadata in PostgreSQL.
            Document savedDocument = documentRepository.save(document);

            return ResponseEntity.ok(savedDocument);

        } catch (IOException e) {

            return ResponseEntity.internalServerError()
                    .body("Failed to upload PDF to Cloudinary.");

        }
    }
    /**
     * Downloads a stored PDF from Cloudinary and extracts its text.
     *
     * Used to test the document processing pipeline.
     */
    @GetMapping("/{id}/process")
    public ResponseEntity<?> processDocument(@PathVariable Long id) {

        try {

            // Find the document metadata from PostgreSQL.
            Document document = documentRepository.findById(id)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Document not found with id: " + id
                            )
                    );

            // Download, extract, and split the PDF into chunks.
            List<String> chunks =
                    documentProcessingService.processDocument(document);

            return ResponseEntity.ok(chunks);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body("Failed to process document.");
        }
    }
}