package com.collegeai.backend.controller;

import com.collegeai.backend.dto.RetrievedChunk;
import com.collegeai.backend.service.DocumentRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final DocumentRetrievalService documentRetrievalService;

    @GetMapping("/search")
    public ResponseEntity<List<RetrievedChunk>> search(
            @RequestParam String query) {

        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<RetrievedChunk> results =
                documentRetrievalService.search(query.trim());

        return ResponseEntity.ok(results);
    }
}