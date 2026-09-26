package com.collegeai.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin document module.
 *
 * Handles college document uploads.
 * PDF processing and RAG integration will be added
 * in later steps.
 */
@RestController
@RequestMapping("/api/admin/documents")
public class AdminDocumentController {

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please select a file to upload.");
        }

        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body("Only PDF files are allowed.");
        }

        return ResponseEntity.ok(
                "PDF uploaded successfully: " + file.getOriginalFilename()
        );
    }
}