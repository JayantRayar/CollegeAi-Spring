package com.collegeai.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "document_index_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_version",
                        columnNames = {"document_id", "version_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DocumentIndexVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The original document this index version belongs to.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    // Version number for this document: 1, 2, 3, etc.
    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    // Whether this version is staging, active, retired, or failed.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentIndexVersionStatus status =
            DocumentIndexVersionStatus.STAGING;

    // When this version was created.
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // When this version became active.
    private LocalDateTime activatedAt;
}