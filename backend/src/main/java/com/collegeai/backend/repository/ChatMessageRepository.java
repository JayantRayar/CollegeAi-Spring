package com.collegeai.backend.repository;

import com.collegeai.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByConversationIdOrderByCreatedAtAscIdAsc(
            Long conversationId
    );
}