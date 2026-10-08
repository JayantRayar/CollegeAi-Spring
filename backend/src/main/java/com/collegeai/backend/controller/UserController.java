package com.collegeai.backend.controller;

import com.collegeai.backend.dto.ChangePasswordRequest;
import com.collegeai.backend.dto.UpdateProfileRequest;
import com.collegeai.backend.dto.UserResponse;
import com.collegeai.backend.entity.User;
import com.collegeai.backend.exception.ResourceNotFoundException;
import com.collegeai.backend.repository.UserRepository;
import com.collegeai.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles authenticated user profile operations.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * Returns the currently authenticated user's profile.
     */
    @GetMapping("/profile")
    public UserResponse getProfile(
            Authentication authentication) {

        return userRepository
                .findByEmail(authentication.getName())
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole()
                ))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));
    }

    /**
     * Updates the currently authenticated user's name.
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {

        User user =
                userRepository
                        .findByEmail(authentication.getName())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                ));

        user.setName(request.getName());

        User updatedUser =
                userRepository.save(user);

        UserResponse response =
                new UserResponse(
                        updatedUser.getId(),
                        updatedUser.getName(),
                        updatedUser.getEmail(),
                        updatedUser.getRole()
                );

        return ResponseEntity.ok(response);
    }
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok().build();
    }
}