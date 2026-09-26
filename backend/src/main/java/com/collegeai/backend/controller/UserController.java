package com.collegeai.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Protected user endpoint.
 *
 * This endpoint is used to verify that a valid JWT
 * is correctly authenticated by Spring Security.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    public String getProfile(Authentication authentication) {

        return "Authenticated user: " + authentication.getName();
    }
}