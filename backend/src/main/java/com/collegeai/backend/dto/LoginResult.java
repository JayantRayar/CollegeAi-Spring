package com.collegeai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Internal authentication result.
 *
 * Contains the access-token response and the refresh token.
 * The refresh token is not directly returned as JSON.
 */
@Getter
@AllArgsConstructor
public class LoginResult {

    private final LoginResponse loginResponse;

    private final String refreshToken;
}