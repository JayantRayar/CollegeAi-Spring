package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;
import org.springframework.ai.vectorstore.filter.Filter.Key;
import org.springframework.ai.vectorstore.filter.Filter.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles storing document chunks and their embeddings
 * in the configured vector store.
 */
@Service
public class DocumentVectorStoreService {

    private final VectorStore vectorStore;

    public DocumentVectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void storeChunks(List<DocumentChunk> chunks) {

        if (chunks.isEmpty()) {
            return;
        }

        List<Document> documents = chunks.stream()
                .map(this::convertToVectorDocument)
                .toList();

        vectorStore.add(documents);


    }

    public void deleteChunks(List<String> chunkIds) {

        if (chunkIds.isEmpty()) {
            return;
        }

        vectorStore.delete(chunkIds);
    }

    public void deleteDocumentChunks(Long documentId) {

        Filter.Expression filterExpression =
                new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("documentId"),
                        new Filter.Value(documentId)
                );

        vectorStore.delete(filterExpression);
    }

    private Document convertToVectorDocument(
            DocumentChunk chunk) {

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(
                "documentId",
                chunk.getDocumentId()
        );

        metadata.put(
                "documentName",
                chunk.getDocumentName()
        );

        metadata.put(
                "pageNumber",
                chunk.getPageNumber()
        );

        metadata.put(
                "chunkNumber",
                chunk.getChunkNumber()
        );

        return new Document(
                chunk.getId(),
                chunk.getText(),
                metadata
        );
    }
}