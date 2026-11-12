package com.collegeai.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class VoiceChatRequest {

    @NotBlank(message = "Question cannot be empty")
    @Size(max = 2000, message = "Question cannot exceed 2000 characters")
    private String question;
}