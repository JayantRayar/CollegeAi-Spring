package com.collegeai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for JWT token generation.
 */
@Configuration
public class JwtConfig {

    /**
     * Creates the JWT encoder using the configured secret key.
     */
    @Bean
    public JwtEncoder jwtEncoder(
            @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
            String secret) {

        SecretKey secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return NimbusJwtEncoder.withSecretKey(secretKey).build();
    }
}