package com.collegeai.backend.service;

import com.collegeai.backend.dto.LoginRequest;
import com.collegeai.backend.dto.LoginResponse;
import com.collegeai.backend.dto.LoginResult;
import com.collegeai.backend.dto.RegisterRequest;
import com.collegeai.backend.dto.UserResponse;
import com.collegeai.backend.entity.User;
import com.collegeai.backend.exception.EmailAlreadyExistsException;
import com.collegeai.backend.exception.InvalidCredentialsException;
import com.collegeai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles authentication-related business logic.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Registers a new user.
     */
    public UserResponse register(RegisterRequest request) {

        // Check whether the email is already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "Email is already registered"
            );
        }

        // Create a new user entity
        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Hash the password before storing it in PostgreSQL
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Save user in PostgreSQL
        User savedUser = userRepository.save(user);

        // Return safe user information without the password
        return new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    /**
     * Authenticates an existing user.
     *
     * Generates:
     * 1. A short-lived JWT access token.
     * 2. A long-lived refresh token.
     *
     * The refresh token is stored in PostgreSQL.
     */
    public LoginResult login(LoginRequest request) {

        // Find the existing user using the email
        User user =
                userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new InvalidCredentialsException(
                                        "Invalid email or password"
                                )
                        );

        // Compare entered password with the stored BCrypt hash
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        // Create safe user information.
        // Password is never included in the response.
        UserResponse userResponse =
                new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole()
                );

        // Generate short-lived JWT access token.
        String token =
                jwtService.generateToken(
                        user.getEmail(),
                        user.getRole()
                );

        // Generate and store long-lived refresh token.
        String refreshToken =
                refreshTokenService
                        .createRefreshToken(user)
                        .getToken();

        // Create the public API response.
        LoginResponse loginResponse =
                new LoginResponse(
                        token,
                        userResponse
                );

        // Return both pieces internally.
        //
        // LoginResponse will be returned as JSON.
        // Refresh token will later be placed
        // inside an HttpOnly cookie by AuthController.
        return new LoginResult(
                loginResponse,
                refreshToken
        );
    }
}