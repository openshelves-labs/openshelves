package com.openshelves.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for handling custom exceptions across the application.
 * Maps exceptions to appropriate HTTP responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link ThirdPartyClientException} thrown during external API calls.
     *
     * @param e the exception
     * @return a 502 Bad Gateway response with a generic error message
     */
    @ExceptionHandler(ThirdPartyClientException.class)
    public ResponseEntity<String> handleThirdPartyClientException(ThirdPartyClientException e) {
        log.error("Third party client exception", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Unable to process your request due to an external service error. Please try again later.");
    }
}
