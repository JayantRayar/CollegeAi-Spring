
package com.collegeai.backend.service;

import com.collegeai.backend.dto.ChatRequest;
import com.collegeai.backend.dto.ChatResponse;
import com.collegeai.backend.dto.ChatSource;
import com.collegeai.backend.entity.ChatConversation;
import com.collegeai.backend.entity.ChatMessage;
import com.collegeai.backend.entity.MessageRole;
import com.collegeai.backend.entity.User;
import com.collegeai.backend.exception.ResourceNotFoundException;
import com.collegeai.backend.repository.ChatConversationRepository;
import com.collegeai.backend.repository.ChatMessageRepository;
import com.collegeai.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.collegeai.backend.dto.ChatMessageResponse;
import com.collegeai.backend.dto.ChatConversationResponse;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ChatPersistenceService {

    private final UserRepository userRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    public ChatPersistenceService(
            UserRepository userRepository,
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository) {

        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    // Transaction 1: Save the user's question.
    @Transactional
    public Long saveUserQuestion(ChatRequest request, String userEmail) {

        String query = request.getQuery().trim();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        ChatConversation conversation;

        if (request.getConversationId() == null) {

            conversation = new ChatConversation();
            conversation.setUser(user);

            conversation.setTitle(
                    query.substring(0, Math.min(query.length(), 80))
            );

            OffsetDateTime now = OffsetDateTime.now();

            conversation.setCreatedAt(now);
            conversation.setUpdatedAt(now);

            conversation = conversationRepository.save(conversation);

        } else {

            conversation = conversationRepository
                    .findByIdAndUserId(
                            request.getConversationId(),
                            user.getId()
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Conversation not found"
                            ));
        }

        ChatMessage userMessage = new ChatMessage();

        userMessage.setConversation(conversation);
        userMessage.setRole(MessageRole.USER);
        userMessage.setContent(query);
        userMessage.setCreatedAt(OffsetDateTime.now());

        messageRepository.save(userMessage);

        return conversation.getId();
    }

    // Transaction 2: Save the assistant's response.
    @Transactional
    public ChatResponse saveAssistantResponse(
            Long conversationId,
            String answer,
            String answerType,
            List<ChatSource> sources) {

        ChatConversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Conversation not found"
                        ));

        ChatMessage assistantMessage = new ChatMessage();

        assistantMessage.setConversation(conversation);
        assistantMessage.setRole(MessageRole.ASSISTANT);
        assistantMessage.setContent(answer);
        assistantMessage.setAnswerType(answerType);
        assistantMessage.setSources(sources);
        assistantMessage.setCreatedAt(OffsetDateTime.now());

        messageRepository.save(assistantMessage);

        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        return new ChatResponse(
                conversation.getId(),
                answer,
                answerType,
                sources
        );
    }
    // Fetch messages only if the conversation belongs to the logged-in user.
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversationMessages(
            Long conversationId,
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        ChatConversation conversation = conversationRepository
                .findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Conversation not found"));

        return messageRepository
                .findAllByConversationIdOrderByCreatedAtAscIdAsc(
                        conversation.getId()
                )
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    // Fetch only conversations belonging to the logged-in user.
    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getUserConversations(
            String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return conversationRepository
                .findAllByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(ChatConversationResponse::from)
                .toList();
    }
}
