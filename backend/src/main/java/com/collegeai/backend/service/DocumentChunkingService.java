package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DocumentChunkingService {

    private final TokenTextSplitter splitter;

    public DocumentChunkingService() {
        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(1000)
                .build();
    }

    public List<DocumentChunk> createChunks(
            String pageText,
            int pageNumber,
            AtomicInteger chunkCounter
    ) {

        Document document = new Document(pageText);

        List<Document> chunks = splitter.apply(List.of(document));

        return chunks.stream()
                .map(chunk -> new DocumentChunk(
                        chunk.getText(),
                        pageNumber,
                        chunkCounter.getAndIncrement()
                ))
                .toList();
    }
}