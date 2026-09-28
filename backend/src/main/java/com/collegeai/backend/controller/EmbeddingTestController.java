package com.collegeai.backend.controller;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * Temporary controller used to verify that
 * Spring AI can generate embeddings using Ollama.
 */
@RestController
public class EmbeddingTestController {

    private final EmbeddingModel embeddingModel;

    public EmbeddingTestController(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generates an embedding for a sample sentence.
     */
    @GetMapping("/api/test/embedding")
    public String testEmbedding() {

        String text = "What is the CSE management quota fee?";

        float[] embedding =
                embeddingModel.embed(text);

        return "Embedding dimensions: "
                + embedding.length
                + "\nFirst 10 values: "
                + Arrays.toString(
                Arrays.copyOf(
                        embedding,
                        Math.min(10, embedding.length)
                )
        );
    }
}