
package com.collegeai.backend.controller;

import com.collegeai.backend.dto.LoginRequest;
import com.collegeai.backend.dto.LoginResponse;
import com.collegeai.backend.dto.LoginResult;
import com.collegeai.backend.dto.RegisterRequest;
import com.collegeai.backend.dto.UserResponse;
import com.collegeai.backend.exception.InvalidRefreshTokenException;
import com.collegeai.backend.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final long REFRESH_TOKEN_MAX_AGE_SECONDS =
            30L * 24 * 60 * 60;

    private final AuthService authService;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String cookieSameSite;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResult loginResult =
                authService.login(request);

        addRefreshTokenCookie(
                response,
                loginResult.getRefreshToken()
        );

        return ResponseEntity.ok(
                loginResult.getLoginResponse()
        );
    }

    /**
     * Generates a new access token using the refresh token
     * stored in the HttpOnly cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken =
                extractRefreshToken(request);

        LoginResult loginResult =
                authService.refreshAccessToken(
                        refreshToken
                );

        addRefreshTokenCookie(
                response,
                loginResult.getRefreshToken()
        );

        return ResponseEntity.ok(
                loginResult.getLoginResponse()
        );
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            String refreshToken = extractRefreshToken(request);
            authService.logout(refreshToken);
        } catch (InvalidRefreshTokenException exception) {
            // No valid session token remains to revoke.
            // Clear the browser cookie regardless.
        } finally {
            clearRefreshTokenCookie(response);
        }

        return ResponseEntity.ok().build();
    }


    /**
     * Extracts the refresh token from the browser cookie.
     */
    private String extractRefreshToken(
            HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            throw new InvalidRefreshTokenException(
                    "Invalid refresh token"
            );
        }

        for (Cookie cookie : cookies) {

            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        throw new InvalidRefreshTokenException(
                "Invalid refresh token"
        );
    }

    /**
     * Creates the HttpOnly refresh-token cookie.
     */
    private void addRefreshTokenCookie(
            HttpServletResponse response,
            String refreshToken) {

        ResponseCookie refreshTokenCookie =
                ResponseCookie
                        .from("refresh_token", refreshToken)
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .path("/api/auth")
                        .maxAge(REFRESH_TOKEN_MAX_AGE_SECONDS)
                        .sameSite(cookieSameSite)
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookie.toString()
        );
    }

    /**
     * Clears the refresh-token cookie from the browser.
     */
    private void clearRefreshTokenCookie(
            HttpServletResponse response) {

        ResponseCookie refreshTokenCookie =
                ResponseCookie
                        .from("refresh_token", "")
                        .httpOnly(true)
                        .secure(cookieSecure)
                        .path("/api/auth")
                        .maxAge(0)
                        .sameSite(cookieSameSite)
                        .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                refreshTokenCookie.toString()
        );
    }
}
