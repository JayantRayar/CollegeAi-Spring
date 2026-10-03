package com.collegeai.backend.controller;

import com.collegeai.backend.dto.DocumentChunk;
import com.collegeai.backend.dto.DocumentProcessingResponse;
import com.collegeai.backend.dto.DocumentResponse;
import com.collegeai.backend.entity.Document;
import com.collegeai.backend.exception.DocumentUploadException;
import com.collegeai.backend.repository.DocumentRepository;
import com.collegeai.backend.service.CloudinaryService;
import com.collegeai.backend.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final CloudinaryService cloudinaryService;
    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new DocumentUploadException(
                    "Please select a file to upload."
            );
        }

        if (!"application/pdf".equals(file.getContentType())) {
            throw new DocumentUploadException(
                    "Only PDF files are allowed."
            );
        }

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

        document.setFileSize(file.getSize());

        Document savedDocument =
                documentRepository.save(document);

        return ResponseEntity.ok(
                DocumentResponse.from(savedDocument)
        );
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<DocumentProcessingResponse> processDocument(
            @PathVariable Long id) throws Exception {

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
    }

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