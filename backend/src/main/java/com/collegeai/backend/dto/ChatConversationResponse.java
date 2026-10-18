package com.collegeai.backend.dto;

import com.collegeai.backend.entity.ChatConversation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class ChatConversationResponse {

    private Long id;
    private String title;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ChatConversationResponse from(ChatConversation conversation) {
        return new ChatConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}