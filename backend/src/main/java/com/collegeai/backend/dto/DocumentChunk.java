package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a processed chunk of a document.
 *
 * Each chunk belongs to a specific document
 * and a specific index version.
 */
@Getter
@AllArgsConstructor
public class DocumentChunk {

    private final String id;

    private final String text;

    private final Long documentId;

    // Identifies the specific index version this chunk belongs to.
    private final Long indexVersionId;

    /**
     * Original PDF filename.
     * Used for source/citation information.
     */
    private final String documentName;

    private final int pageNumber;

    private final int chunkNumber;
}