package com.collegeai.backend.service;

import com.collegeai.backend.dto.RetrievedChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles semantic retrieval from the vector store.
 *
 * This service keeps Spring AI / ChromaDB details
 * away from the REST API layer.
 */
@Service
public class DocumentRetrievalService {

    private final VectorStore vectorStore;

    public DocumentRetrievalService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Searches the vector store for chunks related
     * to the user's question.
     *
     * @param query user's question
     * @return relevant document chunks
     */
    public List<RetrievedChunk> search(String query) {

        List<Document> documents =
                vectorStore.similaritySearch(query);

        return documents.stream()
                .map(this::convertToRetrievedChunk)
                .toList();
    }

    /**
     * Converts Spring AI's Document into our
     * application-level DTO.
     */
    private RetrievedChunk convertToRetrievedChunk(
            Document document) {

        Long documentId =
                ((Number) document.getMetadata()
                        .get("documentId"))
                        .longValue();

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
                pageNumber,
                chunkNumber,
                score
        );
    }
}