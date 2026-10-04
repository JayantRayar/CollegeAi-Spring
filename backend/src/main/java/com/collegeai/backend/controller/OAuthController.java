
package com.collegeai.backend.controller;

import com.collegeai.backend.dto.LoginResponse;
import com.collegeai.backend.dto.OAuthCodeExchangeRequest;
import com.collegeai.backend.dto.UserResponse;
import com.collegeai.backend.entity.User;
import com.collegeai.backend.repository.UserRepository;
import com.collegeai.backend.security.OAuth2AuthorizationCodeService;
import com.collegeai.backend.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuth2AuthorizationCodeService authorizationCodeService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/exchange")
    public ResponseEntity<LoginResponse> exchangeCode(
            @Valid @RequestBody OAuthCodeExchangeRequest request
    ) {

        // Validate and consume the temporary OAuth code.
        // The code can only be used once.
        String email =
                authorizationCodeService.consumeCode(
                        request.getCode()
                );

        // Invalid or expired code.
        if (email == null) {
            return ResponseEntity
                    .badRequest()
                    .build();
        }

        // Find the corresponding local user.
        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "OAuth user was not found in database"
                                )
                        );

        // Generate our application's JWT.
        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        user.getRole()
                );

        // Build the same response structure used by normal login.
        LoginResponse response =
                new LoginResponse(
                        token,
                        new UserResponse(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getRole()
                        )
                );
        return ResponseEntity.ok(response);
    }
}

