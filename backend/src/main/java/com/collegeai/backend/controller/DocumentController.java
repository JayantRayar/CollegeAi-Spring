package com.collegeai.backend.controller;

import com.collegeai.backend.dto.DocumentResponse;
import com.collegeai.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Handles admin document-management requests.
 */
@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Returns all documents uploaded by the admin.
     */
    @GetMapping
    public List<DocumentResponse> getAllDocuments() {
        return documentService.getAllDocuments();
    }
}