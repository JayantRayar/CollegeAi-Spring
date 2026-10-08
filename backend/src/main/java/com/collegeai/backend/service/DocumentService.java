package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentResponse;
import com.collegeai.backend.entity.Document;
import com.collegeai.backend.exception.DocumentUploadException;
import com.collegeai.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Handles document-related business operations.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Returns all uploaded documents.
     *
     * Converts database entities into
     * frontend-safe response DTOs.
     */
    public List<DocumentResponse> getAllDocuments() {

        return documentRepository.findAll()
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    /**
     * Uploads a PDF to Cloudinary and stores
     * its metadata in PostgreSQL.
     */
    public DocumentResponse uploadDocument(
            MultipartFile file) {

        // Validate the file before uploading it.
        validatePdf(file);

        // Upload the PDF to Cloudinary.
        Map uploadResult =
                cloudinaryService.uploadFile(file);

        // Create document metadata.
        Document document =
                new Document();

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

        /*
         * Save document metadata in PostgreSQL.
         *
         * Cloudinary and PostgreSQL are separate systems.
         * If PostgreSQL fails after Cloudinary succeeds,
         * delete the Cloudinary file to prevent an orphaned file.
         */
        try {

            Document savedDocument =
                    documentRepository.save(document);

            // Return frontend-safe DTO.
            return DocumentResponse.from(
                    savedDocument
            );

        } catch (Exception e) {

            // PostgreSQL save failed after Cloudinary upload.
            // Clean up the Cloudinary file.
            cloudinaryService.deleteFile(
                    document.getPublicId()
            );

            // Re-throw the original database error.
            throw e;
        }
    }

    /**
     * Validates an uploaded PDF before
     * sending it to Cloudinary.
     */
    private void validatePdf(
            MultipartFile file) {

        // Check whether a file was actually provided.
        if (file == null || file.isEmpty()) {

            throw new DocumentUploadException(
                    "PDF file is required."
            );
        }

        // Get the original filename.
        String filename =
                file.getOriginalFilename();

        // Make sure the filename exists.
        if (filename == null ||
                filename.isBlank()) {

            throw new DocumentUploadException(
                    "PDF filename is required."
            );
        }

        // Check the file extension.
        if (!filename.toLowerCase()
                .endsWith(".pdf")) {

            throw new DocumentUploadException(
                    "Only PDF files are allowed."
            );
        }
    }
}