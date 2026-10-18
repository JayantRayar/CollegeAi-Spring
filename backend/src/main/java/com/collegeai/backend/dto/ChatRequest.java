package com.collegeai.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Query cannot be empty")
    private String query;

    private Long conversationId;
}