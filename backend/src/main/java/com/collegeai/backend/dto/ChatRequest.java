package com.collegeai.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {


    @NotBlank(message = "Query cannot be empty")
    @Size(max = 2000, message = "Query cannot exceed 2000 characters")
    private String query;

    @Positive(message = "Conversation ID must be a positive number")
    private Long conversationId;
}