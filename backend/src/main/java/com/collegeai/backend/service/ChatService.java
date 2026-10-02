
package com.collegeai.backend.service;
import com.collegeai.backend.dto.ChatRequest;
import com.collegeai.backend.dto.ChatResponse;
import com.collegeai.backend.dto.ChatSource;
import com.collegeai.backend.dto.RetrievedChunk;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final DocumentRetrievalService documentRetrievalService;
    private final ChatClient chatClient;

    public ChatService(
            DocumentRetrievalService documentRetrievalService,
            @Qualifier("googleGenAiChatModel") ChatModel chatModel) {

        this.documentRetrievalService =
                documentRetrievalService;

        this.chatClient =
                ChatClient.create(chatModel);
    }

    public ChatResponse chat(ChatRequest request) {

        String query =
                request.getQuery().trim();

        // Step 1:
        // Search the college PDF knowledge base first.
        List<RetrievedChunk> retrievedChunks =
                documentRetrievalService.search(query);

        // Step 2:
        // Convert retrieved PDF chunks into sources
        // that can safely be returned to the frontend.
        List<ChatSource> sources =
                retrievedChunks.stream()
                        .map(ChatSource::from)
                        .toList();

        String answer;

        // Step 3:
        // If relevant PDF information was found,
        // use that information as the source of truth.
        if (!retrievedChunks.isEmpty()) {

            String context =
                    buildContext(retrievedChunks);

            try {

                answer =
                        chatClient.prompt()
                                .system("""
                            You are the AI assistant for BMS College of Engineering.

                            Use the provided college context as the source of truth.

                            Answer ONLY the user's question.

                            Do not add unrelated information.

                            Do not invent college-specific information.

                            If the requested information is present in the context,
                            give the exact information directly and concisely.

                            College context:

                            %s
                            """.formatted(context))
                                .user(query)
                                .call()
                                .content();

            } catch (Exception e) {

                // Gemini is temporarily unavailable.
                // We already have relevant information from the PDF,
                // so use the best retrieved chunk as a fallback.
                answer = buildFallbackAnswer(retrievedChunks);
            }

        } else {

            // Step 4:
            // No relevant college information was found.
            // Gemini acts as the general AI fallback.
            answer =
                    chatClient.prompt()
                            .system("""
                                    Answer the user's question using your
                                    general knowledge.

                                    Answer only what the user asked.

                                    Keep the answer relevant and concise.

                                    Do not add unnecessary unrelated information.
                                    """)
                            .user(query)
                            .call()
                            .content();
        }

        // Step 5:
        // Return the answer and the PDF sources to the frontend.
        return new ChatResponse(
                answer,
                sources
        );
    }

    /**
     * Combines the retrieved PDF chunks into a single
     * context that can be provided to Gemini.
     */
    private String buildContext(
            List<RetrievedChunk> chunks) {

        return chunks.stream()
                .map(chunk -> """
                        Document: %s
                        Page: %d
                        Content:
                        %s
                        """.formatted(
                        chunk.getDocumentName(),
                        chunk.getPageNumber(),
                        chunk.getText()
                ))
                .reduce(
                        "",
                        (current, next) ->
                                current + "\n" + next
                );
    }
    /**
     * Creates a safe fallback answer directly from the
     * most relevant retrieved PDF chunk when Gemini
     * is temporarily unavailable.
     */
    private String buildFallbackAnswer(
            List<RetrievedChunk> chunks) {

        if (chunks.isEmpty()) {
            return "I could not find relevant information in the college documents.";
        }

        RetrievedChunk bestChunk = chunks.get(0);

        return "According to the college documents:\n\n"
                + bestChunk.getText()
                + "\n\n"
                + "Source: "
                + bestChunk.getDocumentName()
                + ", page "
                + bestChunk.getPageNumber();
    }
}
