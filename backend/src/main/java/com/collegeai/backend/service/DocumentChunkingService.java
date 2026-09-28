package com.collegeai.backend.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits extracted document text into smaller overlapping chunks.
 *
 * These chunks will later be converted into embeddings
 * and stored in ChromaDB for RAG retrieval.
 */
@Service
public class DocumentChunkingService {

    /**
     * Splits text into chunks using a fixed size and overlap.
     *
     * @param text extracted PDF text
     * @return list of text chunks
     */
    public List<String> createChunks(String text) {

        int chunkSize = 1000;
        int overlap = 200;

        List<String> chunks = new ArrayList<>();

        int start = 0;

        while (start < text.length()) {

            int end = Math.min(
                    start + chunkSize,
                    text.length()
            );

            String chunk = text
                    .substring(start, end)
                    .trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end == text.length()) {
                break;
            }

            start = end - overlap;
        }

        return chunks;
    }
}