
package com.collegeai.backend.service;

import com.collegeai.backend.dto.ChatRequest;
import com.collegeai.backend.dto.ChatResponse;
import com.collegeai.backend.dto.ChatSource;
import com.collegeai.backend.dto.RetrievedChunk;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
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
        // Always search the college PDF knowledge base first.
        List<RetrievedChunk> retrievedChunks =
                documentRetrievalService.search(query);

        // Step 2:
        // Convert retrieved PDF chunks into safe
        // source information for the frontend.
        List<ChatSource> sources =
                retrievedChunks.stream()
                        .map(ChatSource::from)
                        .toList();

        String answer;
        String answerType;

        // Step 3:
        // Relevant college information was found.
        if (!retrievedChunks.isEmpty()) {

            answerType = "COLLEGE_KNOWLEDGE";

            String context =
                    buildContext(retrievedChunks);

            try {

                // Step 4:
                // Ask Gemini to formulate a concise answer
                // using the retrieved college information.
                answer =
                        chatClient.prompt()
                                .system("""
                                        You are the AI assistant for
                                        BMS College of Engineering.

                                        The information provided below comes
                                        from the college's knowledge documents.

                                        IMPORTANT RULES:

                                        1. Answer ONLY the user's question.

                                        2. Use the provided college context
                                           as the source of truth for
                                           college-specific information.

                                        3. Do not add unrelated information.

                                        4. Do not provide general information
                                           when the context contains the answer.

                                        5. Do not ask the user for information
                                           that is already available in the context.

                                        6. Never invent or guess
                                           college-specific information.

                                        7. Keep simple factual questions concise.

                                        8. If the user asks for a specific
                                           value, give that value directly.

                                        9. Do not repeat or summarize the
                                           entire college context.

                                        10. Do not include unrelated fees,
                                            courses, facilities, admissions,
                                            placements, hostel information,
                                            or other details unless the user
                                            specifically asks for them.

                                        11. If the context does not contain
                                            enough information to answer the
                                            question, clearly say that the
                                            information is not available in
                                            the college documents.

                                        College context:

                                        %s
                                        """.formatted(context))
                                .user("""
                                        User question:

                                        %s
                                        """.formatted(query))
                                .call()
                                .content();

            } catch (Exception e) {

                // Gemini may temporarily be unavailable.
                // Since relevant college information was already found,
                // use the retrieved PDF content as a safe fallback.

                answer =
                        buildFallbackAnswer(retrievedChunks);
            }

        } else {

            // Step 5:
            // No relevant college information was found.
            // Gemini acts as the general AI fallback.

            answerType = "GENERAL_AI";

            try {

                answer =
                        chatClient.prompt()
                                .system("""
                                        Answer the user's question using
                                        your general knowledge.

                                        Answer only what the user asked.

                                        Keep the answer relevant and concise.

                                        Do not add unnecessary unrelated information.
                                        """)
                                .user(query)
                                .call()
                                .content();

            } catch (Exception e) {

                // Gemini is also unavailable for general questions.
                answer =
                        "The AI service is temporarily unavailable. "
                                + "Please try again shortly.";
            }
        }

        // Step 6:
        // Return the answer, answer type, and PDF sources.
        return new ChatResponse(
                answer,
                answerType,
                sources
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
            return "I could not find relevant information "
                    + "in the college documents.";
        }

        RetrievedChunk bestChunk =
                chunks.get(0);

        return "According to the college documents:\n\n"
                + bestChunk.getText()
                + "\n\n"
                + "Source: "
                + bestChunk.getDocumentName()
                + ", page "
                + bestChunk.getPageNumber();
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
}

