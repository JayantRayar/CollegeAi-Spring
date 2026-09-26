package com.collegeai.backend.controller;


import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @GetMapping("/profile")
    public String getAdminProfile(Authentication authentication){
        return "Authenticated admin : " + authentication.getName();
    }
}
