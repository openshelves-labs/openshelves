package com.openshelves.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/// Global exception handler for mapping application exceptions to HTTP responses.
///
/// Annotated with `@RestControllerAdvice` so it applies across all controllers
/// in the application without requiring per-controller error handling.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /// Handles [ThirdPartyClientException] thrown during external API calls.
    ///
    /// @param e the exception
    /// @return a 502 Bad Gateway response with a generic user-facing error message
    @ExceptionHandler(ThirdPartyClientException.class)
    public ResponseEntity<String> handleThirdPartyClientException(ThirdPartyClientException e) {
        log.error("Third party client exception", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Unable to process your request due to an external service error. Please try again later.");
    }
}
