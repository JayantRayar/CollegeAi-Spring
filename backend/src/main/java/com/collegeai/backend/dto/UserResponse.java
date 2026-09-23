package com.collegeai.backend.dto;

import com.collegeai.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO used to safely send user information in API responses.
 * Password and other sensitive data are never included here.
 */
@Getter
@AllArgsConstructor
public class UserResponse {

    private final Long id;
    private final String name;
    private final String email;
    private final Role role;
}