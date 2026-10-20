package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of verifying whether
 * a generated answer is supported by college documents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroundingResponse {

    private boolean grounded;

    private String reason;
}