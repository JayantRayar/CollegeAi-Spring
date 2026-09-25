package com.collegeai.backend.service;

import com.collegeai.backend.dto.LoginRequest;
import com.collegeai.backend.dto.LoginResponse;
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
     * Authenticates an existing user and generates a JWT.
     */
    public LoginResponse login(LoginRequest request) {

        // Find the existing user using the email
        User user = userRepository.findByEmail(request.getEmail())
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

        // Create safe user information
        // Password is never included in the response
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        // Generate JWT after successful authentication
        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole()
        );

        // Return JWT and user information
        return new LoginResponse(
                token,
                userResponse
        );
    }
}