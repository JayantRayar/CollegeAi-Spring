package com.collegeai.backend.controller;

import com.collegeai.backend.dto.RetrievedChunk;
import com.collegeai.backend.service.DocumentRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for testing and exposing document retrieval.
 *
 * This endpoint will later become part of the
 * complete RAG chatbot flow.
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final DocumentRetrievalService documentRetrievalService;

    /**
     * Searches the knowledge base for relevant document chunks.
     *
     * Example:
     * GET /api/rag/search?query=What is the CSE management quota fee?
     */
    @GetMapping("/search")
    public ResponseEntity<List<RetrievedChunk>> search(
            @RequestParam String query) {

        List<RetrievedChunk> results =
                documentRetrievalService.search(query);

        return ResponseEntity.ok(results);
    }
}