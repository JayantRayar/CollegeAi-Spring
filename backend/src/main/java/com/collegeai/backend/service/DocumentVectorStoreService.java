
package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;

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

    public DocumentVectorStoreService(@Lazy VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Converts document chunks into vector documents
     * and stores them in ChromaDB.
     */
    public void storeChunks(List<DocumentChunk> chunks) {

        if (chunks.isEmpty()) {
            return;
        }

        List<Document> documents = chunks.stream()
                .map(this::convertToVectorDocument)
                .toList();

        vectorStore.add(documents);
    }

    /**
     * Deletes specific vectors using their IDs.
     */
    public void deleteChunks(List<String> chunkIds) {

        if (chunkIds.isEmpty()) {
            return;
        }

        vectorStore.delete(chunkIds);
    }

    /**
     * Deletes all vectors belonging to a document.
     *
     * Retained for existing deletion functionality.
     * Version-specific deletion will be added separately.
     */
    public void deleteDocumentChunks(Long documentId) {

        Filter.Expression filterExpression =
                new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("documentId"),
                        new Filter.Value(documentId)
                );

        vectorStore.delete(filterExpression);
    }

    /**
     * Deletes vectors belonging to a specific index version.
     *
     * This allows us to remove an old or failed version
     * without deleting vectors from other versions.
     */
    public void deleteVersionChunks(Long indexVersionId) {

        Filter.Expression filterExpression =
                new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("indexVersionId"),
                        new Filter.Value(indexVersionId)
                );

        vectorStore.delete(filterExpression);
    }

    /**
     * Converts a DocumentChunk into a Spring AI Document
     * with metadata required for filtering and citations.
     */
    private Document convertToVectorDocument(DocumentChunk chunk) {

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(
                "documentId",
                chunk.getDocumentId()
        );

        // Identifies the index version this chunk belongs to.
        metadata.put(
                "indexVersionId",
                chunk.getIndexVersionId()
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
