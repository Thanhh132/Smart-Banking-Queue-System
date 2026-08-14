package com.sbqs.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    void activeTicketUniqueViolationReturnsConflict() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"ux_tickets_one_active_customer\"");

        ResponseEntity<Map<String, Object>> response =
                new GlobalExceptionHandler().handleDataIntegrityException(violation);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().get("status"));
        assertEquals("Conflict", response.getBody().get("error"));
        assertEquals("ACTIVE_TICKET_EXISTS", response.getBody().get("code"));
        assertEquals(
                "Bạn đang có phiếu chưa hoàn thành. Hãy hoàn thành hoặc hủy phiếu hiện tại trước.",
                response.getBody().get("message"));
    }

    @Test
    void activeTicketServiceRejectionIncludesCodeAndCurrentTicketId() {
        ResponseEntity<Map<String, Object>> response =
                new GlobalExceptionHandler().handleActiveTicketExists(new ActiveTicketExistsException(42L));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACTIVE_TICKET_EXISTS", response.getBody().get("code"));
        assertEquals(42L, response.getBody().get("ticketId"));
    }

    @Test
    void ticketRateLimitReturns429AndRetryDelay() {
        ResponseEntity<Map<String, Object>> response = new GlobalExceptionHandler()
                .handleTicketRateLimit(new TicketRateLimitExceededException("Chờ trước khi lấy số mới", 90));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TICKET_RATE_LIMITED", response.getBody().get("code"));
        assertEquals(90L, response.getBody().get("retryAfterSeconds"));
    }

    @Test
    void fullQueueReturns503AndRetryDelay() {
        ResponseEntity<Map<String, Object>> response = new GlobalExceptionHandler()
                .handleQueueCapacity(new QueueCapacityExceededException(600));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("QUEUE_CAPACITY_REACHED", response.getBody().get("code"));
        assertEquals(600L, response.getBody().get("retryAfterSeconds"));
    }
}
