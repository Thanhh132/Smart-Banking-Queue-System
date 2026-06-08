package com.sbqs.service;

import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.Ticket;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;

    public TicketService(
            TicketRepository ticketRepository,
            QueueMachineServiceMappingRepository mappingRepository) {
        this.ticketRepository = ticketRepository;
        this.mappingRepository = mappingRepository;
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

        QueueMachineServiceMapping mapping = mappingRepository.findFirstByService(ticket.getService())
                .orElseThrow(() -> new RuntimeException("Dịch vụ này chưa được cấu hình máy bốc số"));

        ticket.setQueueMachine(mapping.getQueueMachine());

        return ticketRepository.save(ticket);
    }

    public List<Ticket> getTicketsByStatus(String status) {
        return ticketRepository.findByStatus(status);
    }
}