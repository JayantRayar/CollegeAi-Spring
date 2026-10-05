package com.collegeai.backend.exception;

/**
 * Thrown when a refresh token is missing, invalid,
 * revoked, or expired.
 */
public class InvalidRefreshTokenException
        extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}