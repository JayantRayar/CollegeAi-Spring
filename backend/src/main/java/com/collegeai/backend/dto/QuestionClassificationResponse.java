package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds Gemini's classification of a user's question.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionClassificationResponse {

    private QuestionType type;

    public enum QuestionType {
        COLLEGE_SPECIFIC,
        GENERAL
    }
}