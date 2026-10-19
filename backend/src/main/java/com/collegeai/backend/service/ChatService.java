
package com.collegeai.backend.service;

import com.collegeai.backend.dto.*;
import com.collegeai.backend.entity.ChatConversation;
import com.collegeai.backend.entity.ChatMessage;
import com.collegeai.backend.entity.MessageRole;
import com.collegeai.backend.entity.User;
import com.collegeai.backend.exception.ResourceNotFoundException;
import com.collegeai.backend.repository.ChatConversationRepository;
import com.collegeai.backend.repository.ChatMessageRepository;
import com.collegeai.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ChatService {

    private static final Logger logger =
            LoggerFactory.getLogger(ChatService.class);

    private final DocumentRetrievalService documentRetrievalService;
    private final ChatClient chatClient;

    private final UserRepository userRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(
            DocumentRetrievalService documentRetrievalService,
            @Qualifier("googleGenAiChatModel") ChatModel chatModel,
            UserRepository userRepository,
            ChatConversationRepository chatConversationRepository,
            ChatMessageRepository chatMessageRepository) {

        this.documentRetrievalService = documentRetrievalService;
        this.chatClient = ChatClient.create(chatModel);

        this.userRepository = userRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.chatMessageRepository = chatMessageRepository;
    }
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversationMessages(
            Long conversationId,
            String userEmail) {

        // Find the logged-in user using their email from the JWT.
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Verify that this conversation belongs to the logged-in user.
        ChatConversation conversation = chatConversationRepository
                .findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Conversation not found"));

        // Fetch all messages in chronological order and convert them to DTOs.
        return chatMessageRepository
                .findAllByConversationIdOrderByCreatedAtAscIdAsc(conversation.getId())
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getUserConversations(String userEmail) {

        // Find the currently authenticated user.
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Fetch only this user's conversations, newest activity first.
        return chatConversationRepository
                .findAllByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(ChatConversationResponse::from)
                .toList();
    }

    @Transactional
    public ChatResponse chat(ChatRequest request, String userEmail) {

        String query = request.getQuery().trim();

        // Step 1: Identify the authenticated user using JWT email.
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Step 2: Create a new conversation or retrieve an existing one.
        ChatConversation conversation;

        if (request.getConversationId() == null) {

            conversation = new ChatConversation();

            conversation.setUser(user);

            // Derive the title from the first question.
            conversation.setTitle(
                    query.substring(0, Math.min(query.length(), 80))
            );

            OffsetDateTime now = OffsetDateTime.now();

            conversation.setCreatedAt(now);
            conversation.setUpdatedAt(now);

            conversation = chatConversationRepository.save(conversation);

        } else {

            conversation = chatConversationRepository
                    .findByIdAndUserId(
                            request.getConversationId(),
                            user.getId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Conversation not found"
                            ));
        }

        // Step 3: Save the user's question.
        ChatMessage userMessage = new ChatMessage();

        userMessage.setConversation(conversation);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(query);
        userMessage.setCreatedAt(OffsetDateTime.now());

        chatMessageRepository.save(userMessage);

        // Step 4:
        // Always search the college PDF knowledge base first.
        List<RetrievedChunk> retrievedChunks =
                documentRetrievalService.search(query);

        // Step 5: Convert retrieved chunks into source information.
        List<ChatSource> sources = retrievedChunks.stream()
                .map(ChatSource::from)
                .toList();

        String answer;
        String answerType;

        // Step 6: Generate the answer using college context when available.
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

                if (answer == null || answer.isBlank()) {
                    answer = "I could not generate an answer from "
                            + "the available college information.";
                }

            } catch (Exception e) {

                logger.error("Failed to generate college knowledge answer using Gemini", e);
                // Safe response if Gemini is unavailable.
                answer = buildFallbackAnswer(retrievedChunks);
            }

        } else {

            // Step 7: Use general Gemini knowledge only when
            // no sufficiently relevant college chunks are retrieved.
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

                logger.error("Failed to generate general AI answer using Gemini", e);
                answer = "The AI service is temporarily unavailable. "
                        + "Please try again shortly.";
            }
        }

        // Step 8: Save the assistant's answer and its sources.
        ChatMessage assistantMessage = new ChatMessage();

        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(answer);
        assistantMessage.setAnswerType(answerType);
        assistantMessage.setSources(sources);
        assistantMessage.setCreatedAt(OffsetDateTime.now());

        chatMessageRepository.save(assistantMessage);

        // Step 9: Update the conversation's last activity time.
        conversation.setUpdatedAt(OffsetDateTime.now());

        chatConversationRepository.save(conversation);

        // Step 10: Return the response with conversation ID.
        return new ChatResponse(
                conversation.getId(),
                answer,
                answerType,
                sources
        );
    }

    /**
     * Returns a safe fallback message when Gemini
     * is unavailable while processing college information.
     */
    private String buildFallbackAnswer(List<RetrievedChunk> chunks) {

        return "I'm temporarily unable to generate an answer. "
                + "Please try again shortly.";
    }

    /**
     * Combines retrieved PDF chunks into the context
     * supplied to Gemini.
     */
    private String buildContext(List<RetrievedChunk> chunks) {

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
