package com.collegeai.backend.service;

import com.collegeai.backend.dto.DocumentChunk;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Splits extracted PDF text into chunks suitable
 * for embedding and semantic retrieval.
 *
 * A small amount of previous-page context is included
 * when processing a new page. This helps preserve
 * meaning when content continues across PDF pages.
 */
@Service
public class DocumentChunkingService {

    private static final int PREVIOUS_PAGE_CONTEXT_LENGTH = 300;

    private final TokenTextSplitter splitter;

    public DocumentChunkingService() {

        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(1500)
                .withMinChunkSizeChars(500)
                .withKeepSeparator(true)
                .withPunctuationMarks(
                        List.of('.', '?', '!', '\n', ';', ':')
                )
                .build();
    }

    public List<DocumentChunk> createChunks(
            String pageText,
            String previousPageText,
            Long documentId,
            String documentName,
            int pageNumber,
            AtomicInteger chunkCounter
    ) {
        String combinedText;

        if (previousPageText == null
                || previousPageText.isBlank()) {

            combinedText = pageText;

        } else {

            int contextLength =
                    Math.min(
                            PREVIOUS_PAGE_CONTEXT_LENGTH,
                            previousPageText.length()
                    );

            String previousPageContext =
                    previousPageText.substring(
                            previousPageText.length()
                                    - contextLength
                    );

            combinedText =
                    "[Previous page context]\n"
                            + previousPageContext
                            + "\n\n"
                            + "[Current page]\n"
                            + pageText;
        }

        Document document =
                new Document(combinedText);

        List<Document> chunks =
                splitter.apply(List.of(document));

        return chunks.stream()
                .map(chunk -> {

                    int chunkNumber =
                            chunkCounter.getAndIncrement();

                    String chunkId =
                            "document-" + documentId
                                    + "-page-" + pageNumber
                                    + "-chunk-" + chunkNumber;

                    return new DocumentChunk(
                            chunkId,
                            chunk.getText(),
                            documentId,
                            documentName,
                            pageNumber,
                            chunkNumber
                    );
                })
                .toList();
    }
}