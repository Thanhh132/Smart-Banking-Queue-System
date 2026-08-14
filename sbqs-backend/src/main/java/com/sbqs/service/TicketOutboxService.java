package com.sbqs.service;

import com.sbqs.entity.Ticket;
import com.sbqs.entity.TicketOutboxEvent;
import com.sbqs.repository.TicketOutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class TicketOutboxService {
    public static final String TICKET_CREATED = "TICKET_CREATED";

    private final TicketOutboxEventRepository repository;

    public TicketOutboxService(TicketOutboxEventRepository repository) {
        this.repository = repository;
    }

    public void enqueueTicketCreated(Ticket ticket) {
        TicketOutboxEvent event = new TicketOutboxEvent();
        event.setTicketId(ticket.getTicketId());
        event.setEventType(TICKET_CREATED);
        repository.save(event);
    }
}
