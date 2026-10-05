package com.collegeai.backend.controller;

import com.collegeai.backend.dto.LoginRequest;
import com.collegeai.backend.dto.LoginResponse;
import com.collegeai.backend.dto.LoginResult;
import com.collegeai.backend.dto.RegisterRequest;
import com.collegeai.backend.dto.UserResponse;
import com.collegeai.backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles authentication-related HTTP requests.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final long REFRESH_TOKEN_MAX_AGE_SECONDS =
            30L * 24 * 60 * 60;

    private final AuthService authService;

    /**
     * Registers a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Authenticates an existing user.
     *
     * Access token is returned in the JSON response.
     * Refresh token is stored in an HttpOnly cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResult loginResult =
                authService.login(request);

        ResponseCookie refreshTokenCookie =
                ResponseCookie
                        .from(
                                "refresh_token",
                                loginResult.getRefreshToken()
                        )
                        .httpOnly(true)
                        .secure(false)
                        .path("/api/auth")
                        .maxAge(
                                REFRESH_TOKEN_MAX_AGE_SECONDS
                        )
                        .sameSite("Lax")
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookie.toString()
        );

        return ResponseEntity.ok(
                loginResult.getLoginResponse()
        );
    }
}