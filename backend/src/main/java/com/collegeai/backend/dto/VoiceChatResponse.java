package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceChatResponse {

    private String answer;

    // COLLEGE_KNOWLEDGE or GENERAL_AI
    private String answerType;
}