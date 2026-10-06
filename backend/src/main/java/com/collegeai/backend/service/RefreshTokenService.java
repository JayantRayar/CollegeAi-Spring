package com.collegeai.backend.service;

import com.collegeai.backend.entity.RefreshToken;
import com.collegeai.backend.entity.User;
import com.collegeai.backend.exception.InvalidRefreshTokenException;
import com.collegeai.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 30;

    private final RefreshTokenRepository refreshTokenRepository;

    private final SecureRandom secureRandom =
            new SecureRandom();

    /**
     * Creates a new secure refresh token
     * and stores it in the database.
     */
    public RefreshToken createRefreshToken(User user) {

        byte[] randomBytes = new byte[64];

        secureRandom.nextBytes(randomBytes);

        String token =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(randomBytes);

        RefreshToken refreshToken =
                new RefreshToken();

        refreshToken.setToken(token);
        refreshToken.setUser(user);

        refreshToken.setExpiresAt(
                LocalDateTime.now()
                        .plusDays(
                                REFRESH_TOKEN_EXPIRATION_DAYS
                        )
        );

        return refreshTokenRepository.save(
                refreshToken
        );
    }

    /**
     * Validates a refresh token.
     *
     * Checks:
     * 1. Token exists.
     * 2. Token is not revoked.
     * 3. Token is not expired.
     */
    public RefreshToken validateRefreshToken(
            String token) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new InvalidRefreshTokenException(
                                        "Invalid refresh token"
                                )
                        );

        if (refreshToken.isRevoked()) {

            throw new InvalidRefreshTokenException(
                    "Invalid refresh token"
            );
        }

        if (refreshToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new InvalidRefreshTokenException(
                    "Invalid refresh token"
            );
        }

        return refreshToken;
    }

    /**
     * Revokes the existing refresh token.
     */
    public void revokeRefreshToken(
            RefreshToken refreshToken) {

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(
                refreshToken
        );
    }
}