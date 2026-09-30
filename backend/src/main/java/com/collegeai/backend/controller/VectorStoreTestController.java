package com.collegeai.backend.controller;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VectorStoreTestController {

    private final VectorStore vectorStore;

    public VectorStoreTestController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @GetMapping("/api/test/vector-store")
    public String testVectorStore() {

        Document document = new Document(
                "BMS College of Engineering offers undergraduate and postgraduate courses."
        );

        document.getMetadata().put("documentId", "15");
        document.getMetadata().put("page", "1");
        document.getMetadata().put("category", "admissions");
        document.getMetadata().put("chunkNumber", "1");

        vectorStore.add(List.of(document));

        return "Document added to ChromaDB successfully";
    }

    @GetMapping("/api/test/vector-search")
    public List<Document> testVectorSearch() {

        String query = "What courses does BMS College offer?";

        return vectorStore.similaritySearch(query);
    }
}