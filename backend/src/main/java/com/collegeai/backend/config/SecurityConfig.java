package com.collegeai.backend.config;

import com.collegeai.backend.security.OAuth2SuccessHandler;
import com.collegeai.backend.service.OAuthUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures application security.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            OAuthUserService oauthUserService,
            OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception  {

        System.out.println(">>> SecurityConfig is loaded");

        http
                // Disable CSRF because this backend is using REST APIs.
                .csrf(csrf -> csrf.disable())

                // Define which endpoints are public/protected.
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(
                                "/api/health",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/error"
                        ).permitAll()

                        // Only ADMIN users can access admin APIs.
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Everything else requires authentication.
                        .anyRequest()
                        .authenticated()
                )

                // JWT authentication for REST APIs.
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                )

                // Google OAuth2 / OIDC login.
                .oauth2Login(oauth2 ->
                        oauth2
                                .userInfoEndpoint(userInfo ->
                                        userInfo.oidcUserService(oauthUserService)
                                )
                                .successHandler(oAuth2SuccessHandler)
                );

        return http.build();
    }
}