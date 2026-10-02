package com.collegeai.backend.service;

import com.collegeai.backend.dto.RetrievedChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentRetrievalService {

    private final VectorStore vectorStore;

    private final int topK;
    private final double similarityThreshold;

    public DocumentRetrievalService(
            VectorStore vectorStore,
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

    private RetrievedChunk convertToRetrievedChunk(
            Document document) {

        Long documentId =
                ((Number) document.getMetadata()
                        .get("documentId"))
                        .longValue();

        String documentName =
                (String) document.getMetadata()
                        .get("documentName");

        Integer pageNumber =
                ((Number) document.getMetadata()
                        .get("pageNumber"))
                        .intValue();

        Integer chunkNumber =
                ((Number) document.getMetadata()
                        .get("chunkNumber"))
                        .intValue();

        Double score = document.getScore();

        return new RetrievedChunk(
                document.getText(),
                documentId,
                documentName,
                pageNumber,
                chunkNumber,
                score
        );
    }
}