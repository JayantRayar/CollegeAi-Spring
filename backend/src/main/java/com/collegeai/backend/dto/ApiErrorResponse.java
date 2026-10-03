package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String message;
    private String path;
}