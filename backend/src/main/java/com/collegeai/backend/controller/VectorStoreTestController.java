package com.collegeai.backend.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Temporary controller used to verify
 * semantic search from ChromaDB.
 */
@RestController
public class VectorStoreTestController {

    private final VectorStore vectorStore;

    public VectorStoreTestController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Tests semantic search using a real college-related question.
     */
    @GetMapping("/api/test/vector-search")
    public List<Document> testVectorSearch() {

        String query = "What is the CSE management quota fee?";

        // Spring AI converts the query into an embedding
        // and searches ChromaDB for similar document chunks.
        return vectorStore.similaritySearch(query);
    }
}