package com.collegeai.backend.controller;

import com.collegeai.backend.dto.DocumentChunk;
import com.collegeai.backend.dto.DocumentProcessingResponse;
import com.collegeai.backend.dto.DocumentResponse;
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
 * Handles:
 * - PDF upload
 * - Document processing and vector indexing
 * - Document deletion
 */
@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final CloudinaryService cloudinaryService;
    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;

    /**
     * Uploads a PDF to Cloudinary and stores
     * its metadata in PostgreSQL.
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

            Map uploadResult =
                    cloudinaryService.uploadFile(file);

            Document document = new Document();

            document.setOriginalFilename(
                    file.getOriginalFilename()
            );

            document.setPublicId(
                    (String) uploadResult.get("public_id")
            );

            document.setSecureUrl(
                    (String) uploadResult.get("secure_url")
            );

            document.setResourceType(
                    (String) uploadResult.get("resource_type")
            );

            document.setFileSize(
                    file.getSize()
            );

            Document savedDocument =
                    documentRepository.save(document);

            return ResponseEntity.ok(
                    DocumentResponse.from(savedDocument)
            );

        } catch (IOException e) {

            return ResponseEntity.internalServerError()
                    .body("Failed to upload PDF to Cloudinary.");
        }
    }

    /**
     * Processes and indexes a document.
     *
     * The document is:
     * Cloudinary
     *      ↓
     * PDF download
     *      ↓
     * Text extraction
     *      ↓
     * Chunking
     *      ↓
     * Old vectors removed
     *      ↓
     * New vectors stored in ChromaDB
     *
     * Re-processing the same document removes its
     * previous vectors before storing fresh ones.
     *
     * The API returns only a small stable response DTO
     * instead of exposing the complete chunk contents.
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<?> processDocument(
            @PathVariable Long id) {

        try {

            List<DocumentChunk> chunks =
                    documentProcessingService.processDocument(id);

            String documentName =
                    chunks.isEmpty()
                            ? null
                            : chunks.get(0).getDocumentName();

            DocumentProcessingResponse response =
                    new DocumentProcessingResponse(
                            id,
                            documentName,
                            "PROCESSED",
                            chunks.size()
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body("Failed to process document.");
        }
    }

    /**
     * Deletes a document and its vector data.
     *
     * The service removes:
     * 1. ChromaDB vectors
     * 2. PostgreSQL document metadata
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long id) {

        documentProcessingService.deleteDocument(id);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Document deleted successfully.",
                        "documentId",
                        id
                )
        );
    }
}