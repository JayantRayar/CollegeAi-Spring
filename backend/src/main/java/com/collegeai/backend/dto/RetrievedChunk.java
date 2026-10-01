package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents a document chunk retrieved from the vector store.
 *
 * This DTO prevents Spring AI's internal Document object
 * from becoming part of our public REST API.
 */
@Getter
@AllArgsConstructor
public class RetrievedChunk {

    private String text;

    private Long documentId;

    private Integer pageNumber;

    private Integer chunkNumber;

    private Double score;
}