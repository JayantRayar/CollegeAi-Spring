package com.collegeai.backend.repository;

import com.collegeai.backend.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository
        extends JpaRepository<ChatConversation, Long> {

    List<ChatConversation> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ChatConversation> findByIdAndUserId(Long id, Long userId);
}