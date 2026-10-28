
package com.collegeai.backend.service;

import com.collegeai.backend.entity.Document;
import com.collegeai.backend.entity.DocumentProcessingJob;
import com.collegeai.backend.entity.DocumentProcessingJobStatus;
import com.collegeai.backend.exception.DocumentNotFoundException;
import com.collegeai.backend.repository.DocumentProcessingJobRepository;
import com.collegeai.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentProcessingJobService {

    private final DocumentRepository documentRepository;
    private final DocumentProcessingJobRepository jobRepository;

    @Transactional
    public DocumentProcessingJob createJob(Long documentId) {

        // Find the document that needs processing.
        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new DocumentNotFoundException(
                                        "Document not found with id: "
                                                + documentId
                                )
                        );

        // Prevent duplicate active processing jobs.
        boolean activeJobExists =
                jobRepository.existsByDocument_IdAndStatusIn(
                        documentId,
                        List.of(
                                DocumentProcessingJobStatus.PENDING,
                                DocumentProcessingJobStatus.PROCESSING
                        )
                );

        if (activeJobExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A processing job already exists for this document."
            );
        }

        // Create a new processing job.
        DocumentProcessingJob job =
                new DocumentProcessingJob();

        job.setDocument(document);
        job.setStatus(DocumentProcessingJobStatus.PENDING);
        job.setNextAttemptAt(LocalDateTime.now());

        // Save the job in PostgreSQL.
        return jobRepository.save(job);
    }
}
