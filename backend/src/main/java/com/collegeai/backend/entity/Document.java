package com.collegeai.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stores metadata about documents uploaded by an admin.
 *
 * The actual PDF is stored in Cloudinary.
 * PostgreSQL stores information needed to identify,
 * process, and track the state of that document.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Original name of the uploaded PDF.
     */
    @Column(nullable = false)
    private String originalFilename;

    /**
     * Cloudinary public ID of the uploaded file.
     */
    @Column(nullable = false)
    private String publicId;

    /**
     * Secure HTTPS URL of the PDF stored in Cloudinary.
     */
    @Column(nullable = false, length = 1000)
    private String secureUrl;

    /**
     * Cloudinary resource type.
     * For our PDF upload this will be "raw".
     */
    @Column(nullable = false)
    private String resourceType;

    /**
     * Size of the uploaded file in bytes.
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * Current processing state of the document.
     *
     * UPLOADED   -> PDF is stored but not processed yet.
     * PROCESSING -> PDF is currently being processed.
     * PROCESSED  -> Processing and vector indexing completed.
     * FAILED     -> Processing failed.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    /**
     * Time when the document was uploaded.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
}