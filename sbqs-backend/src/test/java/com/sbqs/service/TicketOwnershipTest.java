package com.sbqs.service;

import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class TicketOwnershipTest {
    @Test
    void customerIdIsAuthoritativeAndRejectsAnotherCustomer() {
        TicketService service = mock(TicketService.class, CALLS_REAL_METHODS);
        User owner = customer(1L, "old@example.com");
        User another = customer(2L, "old@example.com");
        Ticket ticket = new Ticket();
        ticket.setCustomer(owner);
        ticket.setCustomerEmail("old@example.com");

        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(service, "ownsTicket", ticket, owner));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(service, "ownsTicket", ticket, another));
    }

    @Test
    void legacyTicketFallsBackToCaseInsensitiveEmail() {
        TicketService service = mock(TicketService.class, CALLS_REAL_METHODS);
        Ticket ticket = new Ticket();
        ticket.setCustomerEmail("Customer@Example.com");

        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(
                service, "ownsTicket", ticket, customer(3L, "customer@example.com")));
    }

    private User customer(Long id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setRole("CUSTOMER");
        return user;
    }
}
