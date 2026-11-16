package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceChatResponse {

    private String answer;

    /**
     * Possible values:
     * COLLEGE_KNOWLEDGE - Answer supported by college documents
     * GENERAL_AI        - Answer generated using general AI knowledge
     * ERROR             - Retrieval or AI service failure
     */
    private String answerType;
}