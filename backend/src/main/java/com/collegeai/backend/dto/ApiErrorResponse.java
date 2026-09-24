package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Standard response structure used when an API request fails.
 */
@Getter
@AllArgsConstructor
public class ApiErrorResponse {

    private final boolean success;
    private final String message;
    private final int status;
}