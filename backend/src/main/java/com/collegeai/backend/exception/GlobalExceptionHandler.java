package com.collegeai.backend.exception;

import com.collegeai.backend.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles exceptions thrown by REST controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();

        ApiErrorResponse response = new ApiErrorResponse(
                false,
                message,
                HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
    /**
     * Handles invalid login credentials.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception) {

        ApiErrorResponse response = new ApiErrorResponse(
                false,
                exception.getMessage(),
                HttpStatus.UNAUTHORIZED.value()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    /**
     * Handles registration attempts using an existing email.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException exception) {

        ApiErrorResponse response = new ApiErrorResponse(
                false,
                exception.getMessage(),
                HttpStatus.CONFLICT.value()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
}