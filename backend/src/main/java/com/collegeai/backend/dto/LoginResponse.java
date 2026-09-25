package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response returned after successful authentication.
 */
@Getter
@AllArgsConstructor
public class LoginResponse {

    private final String token;
    private final UserResponse user;
}