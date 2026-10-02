package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response returned after a document has been
 * successfully processed and indexed.
 *
 * This DTO keeps the REST API stable and prevents
 * internal chunk data from being exposed to the frontend.
 */
@Getter
@AllArgsConstructor
public class DocumentProcessingResponse {

    private Long documentId;

    private String documentName;

    private String status;

    private int chunkCount;
}