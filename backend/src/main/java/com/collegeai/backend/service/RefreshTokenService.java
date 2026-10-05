package com.collegeai.backend.service;

import com.collegeai.backend.entity.RefreshToken;
import com.collegeai.backend.entity.User;
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

    public RefreshToken validateRefreshToken(
            String token) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid refresh token"
                                )
                        );

        if (refreshToken.isRevoked()) {
            throw new RuntimeException(
                    "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Refresh token has expired"
            );
        }

        return refreshToken;
    }

    public void revokeRefreshToken(
            RefreshToken refreshToken) {

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(
                refreshToken
        );
    }
}