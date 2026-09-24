package com.collegeai.backend.exception;

/**
 * Thrown when a user tries to register with an email
 * that already exists in the database.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}