package com.openshelves.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/// Unchecked exception for a request the API refuses, with the HTTP status
/// and a machine-readable `code` the client can branch on.
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    /// Machine-readable error code, e.g. `LAST_SIGN_IN_METHOD`; `null` to use the default for the status.
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
