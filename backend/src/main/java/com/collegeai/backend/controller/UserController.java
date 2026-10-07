package com.collegeai.backend.controller;

import com.collegeai.backend.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.collegeai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.collegeai.backend.dto.UserResponse;
/**
 * Protected user endpoint.
 *
 * This endpoint is used to verify that a valid JWT
 * is correctly authenticated by Spring Security.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    @GetMapping("/profile")
    public UserResponse getProfile(Authentication authentication) {

        return userRepository
                .findByEmail(authentication.getName())
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole()
                ))
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }
}