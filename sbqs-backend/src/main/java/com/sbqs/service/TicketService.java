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

    public Ticket callNextTicket() {

        Ticket nextTicket = ticketRepository.findFirstByStatusOrderByTicketNumberAsc("WAITING");

        if (nextTicket == null) {
            throw new RuntimeException("Không còn khách đang chờ");
        }

        nextTicket.setStatus("SERVING");

        return ticketRepository.save(nextTicket);
    }

    public Ticket completeTicket(Long ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket"));

        if (!"SERVING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chỉ ticket đang phục vụ mới được hoàn thành");
        }

        ticket.setStatus("COMPLETED");

        return ticketRepository.save(ticket);
    }
}