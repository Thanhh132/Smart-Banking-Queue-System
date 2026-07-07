package com.sbqs.service;

import com.sbqs.exception.LoginRateLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimitServiceTest {
    @Test
    void blocksAfterConfiguredNumberOfFailuresAndResetsAfterSuccess() {
        LoginRateLimitService service = new LoginRateLimitService(3, 15, 15);

        service.recordFailure("user@example.com", "127.0.0.1");
        service.recordFailure("user@example.com", "127.0.0.1");
        assertDoesNotThrow(() -> service.checkAllowed("user@example.com", "127.0.0.1"));

        service.recordFailure("user@example.com", "127.0.0.1");
        assertThrows(LoginRateLimitExceededException.class,
                () -> service.checkAllowed("user@example.com", "127.0.0.1"));

        service.recordSuccess("user@example.com", "127.0.0.1");
        assertDoesNotThrow(() -> service.checkAllowed("user@example.com", "127.0.0.1"));
    }
}
