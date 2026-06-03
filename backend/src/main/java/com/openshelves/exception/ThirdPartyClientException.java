package com.openshelves.exception;

/// Unchecked exception thrown when an error occurs while communicating
/// with a third-party metadata service (e.g., Open Library, Google Books).
public class ThirdPartyClientException extends RuntimeException {

    public ThirdPartyClientException(String message) {
        super(message);
    }

    public ThirdPartyClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
