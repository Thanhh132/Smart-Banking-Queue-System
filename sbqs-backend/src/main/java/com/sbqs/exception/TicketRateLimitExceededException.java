package com.sbqs.exception;

public class TicketRateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public TicketRateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
