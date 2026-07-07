package com.sbqs.exception;

public class LoginRateLimitExceededException extends RuntimeException {
    public LoginRateLimitExceededException(String message) {
        super(message);
    }
}
