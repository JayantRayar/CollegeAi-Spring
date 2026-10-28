
package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChromaIndexVerificationService {

    private static final int PAGE_SIZE = 500;

    private final ChromaApi chromaApi;
    private final ChromaVectorStoreProperties properties;

    public ChromaIndexVerificationService(
            ChromaApi chromaApi,
            ChromaVectorStoreProperties properties
    ) {
        this.chromaApi = chromaApi;
        this.properties = properties;
    }

    public void verifyChunks(
            Long indexVersionId,
            List<DocumentChunk> expectedChunks
    ) {
        if (indexVersionId == null) {
            throw new IllegalArgumentException("Index version ID cannot be null");
        }

        if (expectedChunks == null || expectedChunks.isEmpty()) {
            throw new IllegalStateException("Cannot verify an empty chunk list");
        }

        Set<String> expectedIds = new HashSet<>();

        for (DocumentChunk chunk : expectedChunks) {
            if (chunk.getId() == null || !expectedIds.add(chunk.getId())) {
                throw new IllegalStateException(
                        "Expected chunk IDs contain a null or duplicate ID"
                );
            }
        }

        ChromaApi.Collection collection = chromaApi.getCollection(
                properties.getTenantName(),
                properties.getDatabaseName(),
                properties.getCollectionName()
        );

        if (collection == null) {
            throw new IllegalStateException(
                    "ChromaDB collection was not found"
            );
        }

        Map<String, Object> where = Map.of(
                "indexVersionId", indexVersionId
        );

        Set<String> storedIds = new HashSet<>();
        int offset = 0;

        while (true) {
            ChromaApi.GetEmbeddingsRequest request =
                    new ChromaApi.GetEmbeddingsRequest(
                            null,
                            where,
                            PAGE_SIZE,
                            offset
                    );

            ChromaApi.GetEmbeddingResponse response =
                    chromaApi.getEmbeddings(
                            properties.getTenantName(),
                            properties.getDatabaseName(),
                            collection.id(),
                            request
                    );

            if (response == null || response.ids() == null) {
                throw new IllegalStateException(
                        "ChromaDB returned an invalid verification response"
                );
            }

            List<String> pageIds = response.ids();

            for (String id : pageIds) {
                if (id == null || !storedIds.add(id)) {
                    throw new IllegalStateException(
                            "ChromaDB returned a null or duplicate chunk ID"
                    );
                }
            }

            if (pageIds.size() < PAGE_SIZE) {
                break;
            }

            offset += PAGE_SIZE;
        }

        if (!storedIds.equals(expectedIds)) {
            Set<String> missingIds = new HashSet<>(expectedIds);
            missingIds.removeAll(storedIds);

            Set<String> unexpectedIds = new HashSet<>(storedIds);
            unexpectedIds.removeAll(expectedIds);

            throw new IllegalStateException(
                    "ChromaDB index verification failed. Missing chunks: "
                            + missingIds.size()
                            + ", unexpected chunks: "
                            + unexpectedIds.size()
            );
        }
    }
}
