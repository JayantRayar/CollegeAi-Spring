package com.collegeai.backend.dto;

import com.collegeai.backend.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Public API response for a document.
 *
 * This DTO prevents the JPA entity from being
 * exposed directly through the REST API.
 */
@Getter
@AllArgsConstructor
public class DocumentResponse {

    private Long documentId;
    private String documentName;
    private String status;
    private Long fileSize;
    private LocalDateTime uploadedAt;

    /**
     * Converts the internal Document entity
     * into a frontend-safe API response.
     */
    public static DocumentResponse from(Document document) {

        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getStatus().name(),
                document.getFileSize(),
                document.getUploadedAt()
        );
    }
}