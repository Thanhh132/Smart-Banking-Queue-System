package com.sbqs.service;

import com.sbqs.entity.Ticket;
import com.sbqs.entity.TicketOutboxEvent;
import com.sbqs.event.TicketQueueThresholdNotification;
import com.sbqs.event.DomainEventPublisher;
import com.sbqs.repository.TicketOutboxEventRepository;
import com.sbqs.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TicketOutboxProcessor {
    private static final Logger log = LoggerFactory.getLogger(TicketOutboxProcessor.class);
    private static final int MAX_ATTEMPTS = 10;

    private final TicketOutboxEventRepository outboxRepository;
    private final TicketRepository ticketRepository;
    private final TicketWorkflowService workflowService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DomainEventPublisher domainEventPublisher;

    public TicketOutboxProcessor(
            TicketOutboxEventRepository outboxRepository,
            TicketRepository ticketRepository,
            TicketWorkflowService workflowService,
            ApplicationEventPublisher applicationEventPublisher,
            DomainEventPublisher domainEventPublisher) {
        this.outboxRepository = outboxRepository;
        this.ticketRepository = ticketRepository;
        this.workflowService = workflowService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Scheduled(fixedDelayString = "${sbqs.ticket.outbox.poll-ms:1000}")
    @Transactional
    public void processPending() {
        for (TicketOutboxEvent event : outboxRepository
                .findTop20ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        "PENDING", LocalDateTime.now())) {
            try {
                process(event);
                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());
                event.setLastError(null);
            } catch (RuntimeException exception) {
                int attempts = event.getAttempts() + 1;
                event.setAttempts(attempts);
                event.setLastError(limit(exception.getMessage()));
                if (attempts >= MAX_ATTEMPTS) {
                    event.setStatus("FAILED");
                    log.error("Ticket outbox exhausted retries outboxId={} ticketId={}",
                            event.getOutboxId(), event.getTicketId(), exception);
                } else {
                    event.setAvailableAt(LocalDateTime.now().plusSeconds(Math.min(300, 1L << attempts)));
                    log.warn("Ticket outbox retry scheduled outboxId={} ticketId={} attempt={}",
                            event.getOutboxId(), event.getTicketId(), attempts);
                }
            }
        }
    }

    private void process(TicketOutboxEvent event) {
        if (!TicketOutboxService.TICKET_CREATED.equals(event.getEventType())) {
            throw new IllegalArgumentException("Unsupported ticket outbox event: " + event.getEventType());
        }
        Ticket ticket = ticketRepository.findById(event.getTicketId())
                .orElseThrow(() -> new IllegalStateException("Ticket not found: " + event.getTicketId()));
        if (!"WAITING".equals(ticket.getStatus()) && !"SERVING".equals(ticket.getStatus())) {
            return;
        }

        workflowService.startTicketApproval(ticket);
        if ("WAITING".equals(ticket.getStatus())) {
            long peopleAhead = ticketRepository.countByQueueMachineAndStatusAndTicketNumberLessThan(
                    ticket.getQueueMachine(), "WAITING", ticket.getTicketNumber());
            if (peopleAhead <= 3) {
                applicationEventPublisher.publishEvent(new TicketQueueThresholdNotification(
                        ticket.getTicketId(), ticket.getTicketNumber(), peopleAhead));
            }
        }
        domainEventPublisher.publish(
                "TICKET_CREATED",
                "TICKET",
                ticket.getTicketId().toString(),
                ticket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", ticket.getTicketNumber(),
                        "customerId", ticket.getCustomer().getUserId(),
                        "serviceName", ticket.getService().getServiceName(),
                        "queueMachineName", ticket.getQueueMachine().getMachineName()));
    }

    private String limit(String message) {
        String value = message == null ? "Unknown outbox processing error" : message;
        return value.substring(0, Math.min(1000, value.length()));
    }
}
