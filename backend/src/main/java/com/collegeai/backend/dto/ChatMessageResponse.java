package com.collegeai.backend.dto;

import com.collegeai.backend.entity.ChatMessage;
import com.collegeai.backend.entity.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private MessageRole role;
    private String content;
    private String answerType;
    private List<ChatSource> sources;
    private OffsetDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getAnswerType(),
                message.getSources(),
                message.getCreatedAt()
        );
    }
}