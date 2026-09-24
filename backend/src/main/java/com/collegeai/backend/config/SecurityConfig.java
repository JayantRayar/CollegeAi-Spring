package com.collegeai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures application security and endpoint access rules.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        System.out.println(">>> SecurityConfig is loaded");

        http
                // Disable CSRF for our REST API
                .csrf(csrf -> csrf.disable())

                // Define public and protected endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}