package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds Gemini's answerability evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerabilityResponse {

    private AnswerabilityStatus status;

    private String reason;
}