package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentResponse;
import com.collegeai.backend.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles document-related business operations.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    /**
     * Returns all uploaded documents.
     *
     * Converts database entities into
     * frontend-safe response DTOs.
     */
    public List<DocumentResponse> getAllDocuments() {

        return documentRepository.findAll()
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }
}