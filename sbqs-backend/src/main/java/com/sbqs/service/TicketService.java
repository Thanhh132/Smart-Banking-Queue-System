package com.sbqs.service;

import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.Ticket;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.TicketRepository;
import org.springframework.stereotype.Service;
import com.sbqs.entity.Counter;
import com.sbqs.repository.CounterRepository;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final CounterRepository counterRepository;

    public TicketService(
            TicketRepository ticketRepository,
            QueueMachineServiceMappingRepository mappingRepository,
            CounterRepository counterRepository) {

        this.ticketRepository = ticketRepository;
        this.mappingRepository = mappingRepository;
        this.counterRepository = counterRepository;
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Ticket createTicket(Ticket ticket) {

        QueueMachineServiceMapping mapping = mappingRepository
                .findFirstByService(ticket.getService())
                .orElseThrow(() -> new RuntimeException("Dịch vụ này chưa được cấu hình máy bốc số"));

        QueueMachine queueMachine = mapping.getQueueMachine();

        ticket.setQueueMachine(queueMachine);

        Ticket lastTicket = ticketRepository
                .findTopByQueueMachineOrderByTicketNumberDesc(queueMachine);

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

    public Ticket callNextTicket(Long counterId) {

        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quầy"));

        if (counter.getCurrentTicket() != null
                && "SERVING".equals(counter.getCurrentTicket().getStatus())) {
            throw new RuntimeException("Quầy đang phục vụ khách, vui lòng hoàn thành trước khi gọi số mới");
        }

        if (counter.getQueueMachine() == null) {
            throw new RuntimeException("Quầy chưa được gán máy bốc số");
        }

        Ticket nextTicket = ticketRepository
                .findFirstByQueueMachineAndStatusOrderByTicketNumberAsc(
                        counter.getQueueMachine(),
                        "WAITING");

        if (nextTicket == null) {
            throw new RuntimeException("Không còn khách đang chờ");
        }

        nextTicket.setStatus("SERVING");

        Ticket savedTicket = ticketRepository.save(nextTicket);

        counter.setCurrentTicket(savedTicket);
        counterRepository.save(counter);

        return savedTicket;
    }

    public Ticket completeTicket(Long ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ticket"));

        if (!"SERVING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chỉ ticket đang phục vụ mới được hoàn thành");
        }

        ticket.setStatus("COMPLETED");
        Counter counter = counterRepository.findAll()
                .stream()
                .filter(c -> c.getCurrentTicket() != null
                        && c.getCurrentTicket().getTicketId()
                                .equals(ticketId))
                .findFirst()
                .orElse(null);

        if (counter != null) {
            counter.setCurrentTicket(null);
            counterRepository.save(counter);
        }
        return ticketRepository.save(ticket);
    }
}