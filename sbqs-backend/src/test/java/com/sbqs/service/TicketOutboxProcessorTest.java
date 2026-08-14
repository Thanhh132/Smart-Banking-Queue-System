package com.sbqs.service;

import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.TicketOutboxEvent;
import com.sbqs.event.TicketQueueThresholdNotification;
import com.sbqs.event.DomainEventPublisher;
import com.sbqs.repository.TicketOutboxEventRepository;
import com.sbqs.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketOutboxProcessorTest {

    @Test
    void startsWorkflowAndPublishesNearQueueNotice() {
        TicketOutboxEventRepository outbox = mock(TicketOutboxEventRepository.class);
        TicketRepository tickets = mock(TicketRepository.class);
        TicketWorkflowService workflow = mock(TicketWorkflowService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        DomainEventPublisher domainEvents = mock(DomainEventPublisher.class);
        TicketOutboxEvent event = event();
        Ticket ticket = ticket();
        when(outbox.findTop20ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                org.mockito.ArgumentMatchers.eq("PENDING"), any(LocalDateTime.class)))
                .thenReturn(List.of(event));
        when(tickets.findById(9L)).thenReturn(Optional.of(ticket));
        when(tickets.countByQueueMachineAndStatusAndTicketNumberLessThan(
                ticket.getQueueMachine(), "WAITING", 8)).thenReturn(2L);

        new TicketOutboxProcessor(outbox, tickets, workflow, publisher, domainEvents).processPending();

        verify(workflow).startTicketApproval(ticket);
        verify(publisher).publishEvent(any(TicketQueueThresholdNotification.class));
        verify(domainEvents).publish(
                org.mockito.ArgumentMatchers.eq("TICKET_CREATED"),
                org.mockito.ArgumentMatchers.eq("TICKET"),
                org.mockito.ArgumentMatchers.eq("9"),
                any(),
                any());
        assertEquals("PROCESSED", event.getStatus());
    }

    private TicketOutboxEvent event() {
        TicketOutboxEvent event = new TicketOutboxEvent();
        event.setOutboxId(1L);
        event.setTicketId(9L);
        event.setEventType(TicketOutboxService.TICKET_CREATED);
        return event;
    }

    private Ticket ticket() {
        QueueMachine machine = new QueueMachine();
        machine.setQueueMachineId(3L);
        Ticket ticket = new Ticket();
        ticket.setTicketId(9L);
        ticket.setTicketNumber(8);
        ticket.setQueueMachine(machine);
        com.sbqs.entity.Branch branch = new com.sbqs.entity.Branch();
        branch.setBranchId(2L);
        ticket.setBranch(branch);
        com.sbqs.entity.Services service = new com.sbqs.entity.Services();
        service.setServiceName("Chuyển khoản");
        ticket.setService(service);
        com.sbqs.entity.User customer = new com.sbqs.entity.User();
        customer.setUserId(5L);
        ticket.setCustomer(customer);
        machine.setMachineName("Máy A");
        ticket.setStatus("WAITING");
        return ticket;
    }
}
