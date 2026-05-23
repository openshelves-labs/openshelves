package com.openshelves.exception;

/**
 * Exception thrown when an error occurs while communicating with a third-party metadata service.
 */
public class ThirdPartyClientException extends RuntimeException {

    public ThirdPartyClientException(String message) {
        super(message);
    }

    public ThirdPartyClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
