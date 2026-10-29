package com.collegeai.backend.service;

// Carries the IDs needed after a job has been claimed.
public record ClaimedDocumentJob(
        Long jobId,
        Long documentId
) {
}