package com.sbqs.exception;

public class QueueCapacityExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    public QueueCapacityExceededException(long retryAfterSeconds) {
        super("Hàng đợi tại máy bốc số này đã đầy. Vui lòng thử lại sau hoặc chọn chi nhánh khác.");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
