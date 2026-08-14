package com.sbqs.service;

import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.exception.ActiveTicketExistsException;
import com.sbqs.exception.TicketRateLimitExceededException;
import com.sbqs.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

class TicketOwnershipTest {
    private static final String IDEMPOTENCY_KEY = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    void customerIdIsAuthoritativeAndRejectsAnotherCustomer() {
        TicketService service = mock(TicketService.class, CALLS_REAL_METHODS);
        User owner = customer(1L, "old@example.com");
        User another = customer(2L, "old@example.com");
        Ticket ticket = new Ticket();
        ticket.setCustomer(owner);

        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(service, "ownsTicket", ticket, owner));
        assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(service, "ownsTicket", ticket, another));
    }

    @Test
    void legacyTicketWithoutCustomerIdFailsClosed() {
        TicketService service = mock(TicketService.class, CALLS_REAL_METHODS);
        Ticket ticket = new Ticket();

        assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(
                service, "ownsTicket", ticket, customer(3L, "customer@example.com")));
    }

    @Test
    void currentAndActiveTicketQueriesUseAuthenticatedCustomerId() {
        Fixture fixture = fixture();
        User customer = customer(11L, "current@example.com");
        Ticket ticket = ownedTicket(customer, "SERVING");
        when(fixture.currentUser.requireUser()).thenReturn(customer);
        when(fixture.tickets.findFirstByCustomerUserIdAndStatusInOrderByCreatedAtDesc(
                eq(11L), anyList())).thenReturn(Optional.of(ticket));

        assertSame(ticket, fixture.service.getCurrentCustomerTicket());
        verify(fixture.tickets).findFirstByCustomerUserIdAndStatusInOrderByCreatedAtDesc(
                eq(11L), eq(List.of("WAITING", "SERVING")));

        when(fixture.tickets.findByCustomerUserIdAndStatusIn(eq(11L), anyList()))
                .thenReturn(List.of(ticket));
        when(fixture.users.findByIdForTicketIssuing(11L)).thenReturn(Optional.of(customer));
        ActiveTicketExistsException exception = assertThrows(
                ActiveTicketExistsException.class,
                () -> fixture.service.createTicket(new Ticket(), IDEMPOTENCY_KEY));
        assertSame(ticket.getTicketId(), exception.getTicketId());
        verify(fixture.users).findByIdForTicketIssuing(11L);
        verify(fixture.tickets).findByCustomerUserIdAndStatusIn(
                eq(11L), eq(List.of("WAITING", "SERVING")));
    }

    @Test
    void repeatedIdempotencyKeyReturnsTheOriginalTicketBeforeActiveTicketCheck() {
        Fixture fixture = fixture();
        User customer = customer(31L, "customer@example.com");
        Ticket original = ownedTicket(customer, "WAITING");
        original.setTicketId(301L);
        when(fixture.currentUser.requireUser()).thenReturn(customer);
        when(fixture.users.findByIdForTicketIssuing(31L)).thenReturn(Optional.of(customer));
        when(fixture.tickets.findByCustomerUserIdAndIdempotencyKey(31L, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(original));

        Ticket result = fixture.service.createTicket(new Ticket(), IDEMPOTENCY_KEY);

        assertSame(original, result);
        verify(fixture.tickets, never()).findByCustomerUserIdAndStatusIn(anyLong(), anyList());
        verify(fixture.tickets, never()).save(any());
    }

    @Test
    void enforcesCooldownAfterCustomerCancelsATicket() {
        Fixture fixture = fixture();
        User customer = customer(41L, "cooldown@example.com");
        Ticket cancelled = ownedTicket(customer, "CANCELLED");
        cancelled.setCancelledAt(LocalDateTime.now().minusSeconds(10));
        when(fixture.currentUser.requireUser()).thenReturn(customer);
        when(fixture.users.findByIdForTicketIssuing(41L)).thenReturn(Optional.of(customer));
        when(fixture.tickets.findFirstByCustomerUserIdAndStatusAndCancelledAtIsNotNullOrderByCancelledAtDesc(
                41L, "CANCELLED")).thenReturn(Optional.of(cancelled));

        TicketRateLimitExceededException exception = assertThrows(
                TicketRateLimitExceededException.class,
                () -> fixture.service.createTicket(new Ticket(), IDEMPOTENCY_KEY));

        assertTrue(exception.getRetryAfterSeconds() > 0);
        verify(fixture.tickets, never()).save(any());
    }

    @Test
    void limitsTicketIssuingAttemptsPerMinute() {
        Fixture fixture = fixture();
        User customer = customer(51L, "rate@example.com");
        when(fixture.currentUser.requireUser()).thenReturn(customer);
        when(fixture.users.findByIdForTicketIssuing(51L)).thenReturn(Optional.of(customer));
        when(fixture.tickets.countByCustomerUserIdAndCreatedAtGreaterThanEqual(eq(51L), any()))
                .thenReturn(3L);

        assertThrows(
                TicketRateLimitExceededException.class,
                () -> fixture.service.createTicket(new Ticket(), IDEMPOTENCY_KEY));
        verify(fixture.tickets, never()).save(any());
    }

    @Test
    void customerCanTrackOwnTicketButCannotTrackOrCancelAnotherCustomersTicket() {
        Fixture fixture = fixture();
        User customerA = customer(21L, "same@example.com");
        User customerB = customer(22L, "same@example.com");
        Ticket ticketA = ownedTicket(customerA, "SERVING");
        Ticket ticketB = ownedTicket(customerB, "WAITING");
        ticketA.setTicketId(101L);
        ticketB.setTicketId(102L);
        when(fixture.currentUser.requireUser()).thenReturn(customerA);
        when(fixture.tickets.findById(101L)).thenReturn(Optional.of(ticketA));
        when(fixture.tickets.findById(102L)).thenReturn(Optional.of(ticketB));

        assertEquals(101L, fixture.service.trackCustomerTicket(101L).ticketId());
        assertThrows(RuntimeException.class, () -> fixture.service.trackCustomerTicket(102L));
        assertThrows(RuntimeException.class, () -> fixture.service.cancelTicket(102L));
        verify(fixture.tickets, never()).save(ticketB);
    }

    private Ticket ownedTicket(User owner, String status) {
        Ticket ticket = new Ticket();
        ticket.setCustomer(owner);
        ticket.setTicketNumber(1);
        ticket.setStatus(status);
        return ticket;
    }

    private Fixture fixture() {
        TicketRepository tickets = mock(TicketRepository.class);
        UserRepository users = mock(UserRepository.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        TicketService service = new TicketService(
                tickets,
                users,
                mock(QueueMachineServiceMappingRepository.class),
                mock(QueueMachineRepository.class),
                mock(CounterRepository.class),
                mock(HistoryService.class),
                mock(BranchRepository.class),
                mock(ServiceRepository.class),
                currentUser,
                mock(CounterSessionRepository.class),
                mock(TicketWorkflowService.class),
                mock(TicketOutboxService.class),
                mock(com.sbqs.event.DomainEventPublisher.class),
                mock(PreparedTransactionService.class),
                mock(BranchOperatingHoursService.class),
                mock(ApplicationEventPublisher.class),
                3,
                5,
                120,
                100,
                600,
                "Asia/Ho_Chi_Minh");
        return new Fixture(service, tickets, users, currentUser);
    }

    private record Fixture(
            TicketService service,
            TicketRepository tickets,
            UserRepository users,
            CurrentUserService currentUser) { }

    private User customer(Long id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setRole("CUSTOMER");
        return user;
    }
}
