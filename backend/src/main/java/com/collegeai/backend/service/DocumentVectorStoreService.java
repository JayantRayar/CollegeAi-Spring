package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

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

    /**
     * Stores document chunks in the vector store.
     *
     * Spring AI automatically generates embeddings
     * using the configured EmbeddingModel before
     * storing the documents in ChromaDB.
     */
    public void storeChunks(List<DocumentChunk> chunks) {

        List<Document> documents = chunks.stream()
                .map(this::convertToVectorDocument)
                .toList();

        vectorStore.add(documents);
    }

    /**
     * Converts our DocumentChunk object into
     * Spring AI's Document object.
     */
    private Document convertToVectorDocument(
            DocumentChunk chunk) {

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("documentId", chunk.getDocumentId());
        metadata.put("pageNumber", chunk.getPageNumber());
        metadata.put("chunkNumber", chunk.getChunkNumber());

        return new Document(
                chunk.getText(),
                metadata
        );
    }
}