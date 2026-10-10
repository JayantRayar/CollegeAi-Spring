
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

        String query = request.getQuery().trim();

        // Step 1:
        // Always search the college PDF knowledge base first.
        List<RetrievedChunk> retrievedChunks =
                documentRetrievalService.search(query);

        // Step 2:
        // Convert retrieved PDF chunks into source information
        // that can be returned to the frontend.
        List<ChatSource> sources =
                retrievedChunks.stream()
                        .map(ChatSource::from)
                        .toList();

        String answer;
        String answerType;

        // Step 3:
        // If chunks were retrieved, ask Gemini to answer
        // using only facts supported by the college context.
        if (!retrievedChunks.isEmpty()) {

            answerType = "COLLEGE_KNOWLEDGE";

            String context = buildContext(retrievedChunks);

            try {

                answer = chatClient.prompt()
                        .system("""
                                You are the AI assistant for
                                BMS College of Engineering (BMSCE).

                                The provided context comes from the
                                college's uploaded knowledge documents.

                                IMPORTANT RULES:

                                1. Answer only the user's question.

                                2. Use the provided college context
                                   as the source of truth for
                                   college-specific facts.

                                3. Do not invent or guess college-specific
                                   information.

                                4. Before answering, determine whether
                                   the context actually supports the
                                   facts requested by the user.

                                5. If the context supports only part
                                   of the answer, provide only the
                                   supported information and clearly
                                   explain what cannot be determined
                                   from the documents.

                                6. If the requested fact is not
                                   supported by the context, clearly
                                   state that the information is not
                                   available in the college documents.

                                7. Never infer a personal outcome,
                                   guarantee, hostel room assignment,
                                   admission result, or individual
                                   placement package from general
                                   college statistics.

                                8. General statistics such as average
                                   and highest placement packages
                                   must not be presented as guarantees
                                   for an individual student.

                                9. Keep simple factual answers concise.
                                   If the user asks for a specific
                                   value that is present in the context,
                                   provide it directly.

                                10. Do not add unrelated information
                                    about fees, courses, facilities,
                                    admissions, placements, hostels,
                                    or other topics.

                                11. Do not ask for information that
                                    is already available in the context.

                                12. Do not treat semantic similarity
                                    alone as proof that the context
                                    contains the requested answer.

                                College context:

                                %s
                                """.formatted(context))
                        .user("""
                                User question:

                                %s
                                """.formatted(query))
                        .call()
                        .content();

                // Handle an unexpectedly empty model response.
                if (answer == null || answer.isBlank()) {
                    answer = "I could not generate an answer from "
                            + "the available college information.";
                }

            } catch (Exception e) {

                // If Gemini is unavailable, use the retrieved
                // document content as a fallback.
                answer = buildFallbackAnswer(retrievedChunks);
            }

        } else {

            // Step 4:
            // If no chunks pass the retrieval threshold,
            // use Gemini's general knowledge.
            answerType = "GENERAL_AI";

            try {

                answer = chatClient.prompt()
                        .system("""
                                You are a helpful general AI assistant.

                                Answer the user's question accurately,
                                clearly, and concisely.

                                Do not invent personal information,
                                private institutional records, or
                                guaranteed future outcomes.

                                Do not claim that information comes
                                from BMSCE documents because no
                                college-document chunks were retrieved.

                                If the user asks for an individual
                                outcome that cannot be known with
                                certainty, explain the uncertainty
                                clearly.

                                Answer only what the user asked.
                                """)
                        .user(query)
                        .call()
                        .content();

                if (answer == null || answer.isBlank()) {
                    answer = "The AI service could not generate an answer. "
                            + "Please try again shortly.";
                }

            } catch (Exception e) {

                // Safe response if the general AI call fails.
                answer = "The AI service is temporarily unavailable. "
                        + "Please try again shortly.";
            }
        }

        // Step 5:
        // Preserve the existing API response structure.
        return new ChatResponse(
                answer,
                answerType,
                sources
        );
    }

    /**
     * Creates a fallback answer directly from the best
     * retrieved PDF chunk when Gemini is unavailable.
     */
    private String buildFallbackAnswer(List<RetrievedChunk> chunks) {
        return "I'm temporarily unable to generate an answer. "
                + "Please try again shortly.";
    }

    /**
     * Combines retrieved PDF chunks into the context
     * supplied to Gemini.
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
                        (current, next) -> current + "\n" + next
                );
    }
}

