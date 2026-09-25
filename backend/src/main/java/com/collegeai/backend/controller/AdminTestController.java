package com.collegeai.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary endpoint used to test ADMIN role authorization.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminTestController {

    /**
     * Accessible only to users with the ADMIN role.
     */
    @GetMapping("/test")
    public String adminTest() {
        return "ADMIN authorization successful";
    }
}