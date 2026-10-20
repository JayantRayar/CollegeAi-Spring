
package com.collegeai.backend.service;

import com.collegeai.backend.dto.GroundingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Verifies whether every factual claim in a generated college answer
 * is supported by the retrieved college document context.
 *
 * This service uses Gemini for grounding verification.
 */
@Service
@Slf4j
public class AnswerGroundingService {

    private final ChatClient chatClient;

    public AnswerGroundingService(
            @Qualifier("googleGenAiChatModel") ChatModel chatModel
    ) {
        this.chatClient = ChatClient.create(chatModel);
    }

    /**
     * Checks whether the generated answer is supported by
     * the supplied college document context.
     *
     * @param question       user's original question
     * @param context        retrieved college document context
     * @param generatedAnswer answer generated using retrieved context
     * @return grounding verification result
     */
    public GroundingResponse verifyAnswer(
            String question,
            String context,
            String generatedAnswer
    ) {

        String systemPrompt = """
                You are a strict answer-grounding verifier.

                Your task is to verify whether every factual claim
                in the generated answer is supported by the
                provided college document context.

                RULES:
                1. Use ONLY the provided college document context.
                2. Do not use your general knowledge.
                3. Do not assume or infer facts that are not explicitly
                   supported by the context.
                4. Every factual claim must be supported by the context.
                5. If even one important factual claim is unsupported,
                   mark the answer as not grounded.
                6. If the answer contains information beyond the
                   supplied context, mark it as not grounded.
                7. A statement that information is unavailable in
                   the documents is acceptable.
                8. Return a structured response containing:
                   - grounded: true or false
                   - reason: a short explanation.

                Be strict. When uncertain, return grounded=false.
                """;

        String userPrompt = """
                USER QUESTION:
                %s

                COLLEGE DOCUMENT CONTEXT:
                %s

                GENERATED ANSWER:
                %s

                Verify whether every factual claim in the generated
                answer is supported by the college document context.
                """.formatted(
                question,
                context,
                generatedAnswer
        );

        log.info("Starting grounding verification for college answer.");

        GroundingResponse response = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(GroundingResponse.class);

        if (response == null) {
            log.error("Grounding verifier returned a null response.");
            throw new IllegalStateException(
                    "Grounding verification returned no result."
            );
        }

        log.info(
                "Grounding verification completed. Grounded: {}",
                response.isGrounded()
        );

        return response;
    }
}
