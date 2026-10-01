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
     * Each Spring AI Document receives the deterministic
     * ID generated during document chunking.
     */
    public void storeChunks(List<DocumentChunk> chunks) {

        if (chunks.isEmpty()) {
            return;
        }

        List<Document> documents = chunks.stream()
                .map(this::convertToVectorDocument)
                .toList();

        // Spring AI generates embeddings and stores
        // the documents in ChromaDB.
        vectorStore.add(documents);
    }

    /**
     * Converts our application-level DocumentChunk
     * into Spring AI's Document.
     */
    private Document convertToVectorDocument(
            DocumentChunk chunk) {

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("documentId", chunk.getDocumentId());
        metadata.put("pageNumber", chunk.getPageNumber());
        metadata.put("chunkNumber", chunk.getChunkNumber());

        /*
         * Use the deterministic chunk ID as the
         * identity of the vector document.
         */
        return new Document(
                chunk.getId(),
                chunk.getText(),
                metadata
        );
    }
}