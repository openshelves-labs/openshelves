package com.openshelves.exception;

public class ThirdPartyClientException extends RuntimeException {

    public ThirdPartyClientException(String message) {
        super(message);
    }

    public ThirdPartyClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
