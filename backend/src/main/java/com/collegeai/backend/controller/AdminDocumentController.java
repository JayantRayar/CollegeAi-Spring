package com.collegeai.backend.controller;

import com.collegeai.backend.dto.DocumentDeleteResponse;
import com.collegeai.backend.dto.DocumentProcessingJobResponse;
import com.collegeai.backend.entity.DocumentProcessingJob;
import com.collegeai.backend.service.DocumentProcessingJobService;
import com.collegeai.backend.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final DocumentProcessingJobService jobService;
    private final DocumentProcessingService documentProcessingService;

    // Enqueue a document for background processing.
    @PostMapping("/{id}/process")
    public ResponseEntity<DocumentProcessingJobResponse> processDocument(
            @PathVariable Long id) {

        DocumentProcessingJob job = jobService.createJob(id);

        DocumentProcessingJobResponse response =
                DocumentProcessingJobResponse.from(job);

        // 202 Accepted means the job was queued, not completed.
        return ResponseEntity.accepted().body(response);
    }

    // Delete a document.
    @DeleteMapping("/{id}")
    public ResponseEntity<DocumentDeleteResponse> deleteDocument(
            @PathVariable Long id) {

        documentProcessingService.deleteDocument(id);

        DocumentDeleteResponse response =
                new DocumentDeleteResponse(
                        "Document deleted successfully.",
                        id
                );

        return ResponseEntity.ok(response);
    }
}