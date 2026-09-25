package com.collegeai.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller used to verify JWT authentication.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * Protected endpoint.
     * Only authenticated users can access this endpoint.
     */
    @GetMapping("/protected")
    public String protectedEndpoint() {
        return "JWT authentication successful";
    }
}