package com.collegeai.backend.service;

import com.collegeai.backend.dto.RetrievedChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import java.util.List;
import java.util.Map;

@Service
public class DocumentRetrievalService {

    private final VectorStore vectorStore;

    private final int topK;
    private final double similarityThreshold;

    public DocumentRetrievalService(
            @Lazy VectorStore vectorStore,
            @Value("${rag.retrieval.top-k:5}") int topK,
            @Value("${rag.retrieval.similarity-threshold:0.50}") double similarityThreshold
    ) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public List<RetrievedChunk> search(String query) {

        SearchRequest searchRequest =
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build();

        List<Document> documents =
                vectorStore.similaritySearch(searchRequest);

        return documents.stream()
                .map(this::convertToRetrievedChunk)
                .toList();
    }

    private RetrievedChunk convertToRetrievedChunk(Document document) {

        Map<String, Object> metadata = document.getMetadata();

        Long documentId = requireLong(metadata, "documentId");
        String documentName = requireString(metadata, "documentName");
        Integer pageNumber = requireInteger(metadata, "pageNumber");
        Integer chunkNumber = requireInteger(metadata, "chunkNumber");

        return new RetrievedChunk(
                document.getText(),
                documentId,
                documentName,
                pageNumber,
                chunkNumber,
                document.getScore()
        );
    }

    private Long requireLong(Map<String, Object> metadata, String key) {

        Object value = requireMetadataValue(metadata, key);

        if (!(value instanceof Number number)) {
            throw invalidMetadata(key, "a numeric value");
        }

        return number.longValue();
    }

    private Integer requireInteger(Map<String, Object> metadata, String key) {

        Object value = requireMetadataValue(metadata, key);

        if (!(value instanceof Number number)) {
            throw invalidMetadata(key, "a numeric value");
        }

        long numericValue = number.longValue();

        if (numericValue < Integer.MIN_VALUE
                || numericValue > Integer.MAX_VALUE
                || number.doubleValue() != numericValue) {
            throw invalidMetadata(key, "a valid integer");
        }

        return (int) numericValue;
    }

    private String requireString(Map<String, Object> metadata, String key) {

        Object value = requireMetadataValue(metadata, key);

        if (!(value instanceof String text) || text.isBlank()) {
            throw invalidMetadata(key, "a non-empty string");
        }

        return text;
    }

    private Object requireMetadataValue(Map<String, Object> metadata, String key) {

        if (metadata == null
                || !metadata.containsKey(key)
                || metadata.get(key) == null) {
            throw invalidMetadata(key, "a present value");
        }

        return metadata.get(key);
    }

    private IllegalStateException invalidMetadata(String key, String expected) {

        return new IllegalStateException(
                "Invalid ChromaDB document metadata: '"
                        + key + "' must be " + expected + "."
        );
    }
}