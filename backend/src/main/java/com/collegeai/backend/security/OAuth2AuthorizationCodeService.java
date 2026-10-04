package com.collegeai.backend.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuth2AuthorizationCodeService {

    private static final long CODE_EXPIRATION_MILLIS = 60_000;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, AuthorizationCodeData> codes =
            new ConcurrentHashMap<>();

    /**
     * Creates a secure, temporary, one-time OAuth authorization code.
     *
     * @param email email of the authenticated user
     * @return temporary authorization code
     */
    public String createCode(String email) {

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        String code = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        codes.put(
                code,
                new AuthorizationCodeData(
                        email,
                        System.currentTimeMillis()
                )
        );

        return code;
    }

    /**
     * Validates and consumes an authorization code.
     *
     * The code is removed immediately, so it can only be used once.
     *
     * @param code temporary authorization code
     * @return user email if valid, otherwise null
     */
    public String consumeCode(String code) {

        AuthorizationCodeData data = codes.remove(code);

        // Code does not exist or was already used.
        if (data == null) {
            return null;
        }

        long age =
                System.currentTimeMillis()
                        - data.createdAt();

        // Code has expired.
        if (age > CODE_EXPIRATION_MILLIS) {
            return null;
        }

        return data.email();
    }

    /**
     * Stores the email and creation time for an authorization code.
     */
    private record AuthorizationCodeData(
            String email,
            long createdAt
    ) {
    }
}

