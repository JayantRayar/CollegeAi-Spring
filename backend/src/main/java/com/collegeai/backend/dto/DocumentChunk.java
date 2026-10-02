package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a processed chunk of a document.
 *
 * Each chunk has a deterministic ID so the same
 * document can be re-indexed without creating duplicates.
 */
@Getter
@AllArgsConstructor
public class DocumentChunk {

    private final String id;

    private final String text;

    private final Long documentId;

    /**
     * Original PDF filename.
     * Used later for source/citation information.
     */
    private final String documentName;

    private final int pageNumber;

    private final int chunkNumber;
}