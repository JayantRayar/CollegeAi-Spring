package com.collegeai.backend.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Splits extracted document text into smaller chunks
 * using Spring AI's token-based text splitter.
 *
 * These chunks will later be converted into embeddings
 * and stored in ChromaDB for RAG retrieval.
 */
@Service
public class DocumentChunkingService {

    /**
     * Splits extracted text into token-based chunks.
     *
     * @param text extracted PDF text
     * @return list of text chunks
     */
    public List<String> createChunks(String text) {

        // Convert the extracted text into a Spring AI Document.
        Document document = new Document(text);

        // Create a token-based text splitter.
        TokenTextSplitter splitter =
                TokenTextSplitter.builder()
                        .withChunkSize(1000)
                        .build();

        // Split the document into smaller documents.
        List<Document> chunks =
                splitter.apply(List.of(document));

        // Convert Spring AI Documents back into plain text.
        return chunks.stream()
                .map(Document::getText)
                .toList();
    }
}