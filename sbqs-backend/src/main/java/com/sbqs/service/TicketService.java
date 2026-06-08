package com.sbqs.service;

import com.sbqs.entity.Ticket;
import com.sbqs.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Ticket createTicket(Ticket ticket) {

        Ticket lastTicket = ticketRepository.findTopByOrderByTicketIdDesc();

        int nextTicketNumber;

        if (lastTicket == null) {
            nextTicketNumber = 1;
        } else {
            nextTicketNumber = lastTicket.getTicketNumber() + 1;
        }

        ticket.setTicketNumber(nextTicketNumber);

        return ticketRepository.save(ticket);
    }

    public List<Ticket> getTicketsByStatus(String status) {
        return ticketRepository.findByStatus(status);
    }
}