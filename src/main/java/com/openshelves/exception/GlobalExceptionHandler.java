package com.openshelves.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ThirdPartyClientException.class)
    public ResponseEntity<String> handleThirdPartyClientException(ThirdPartyClientException e) {
        log.error("Third party client exception", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Unable to process your request due to an external service error. Please try again later.");
    }
}
