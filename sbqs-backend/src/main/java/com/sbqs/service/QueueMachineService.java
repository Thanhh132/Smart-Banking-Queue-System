package com.sbqs.service;

import com.sbqs.event.DomainEventPublisher;
import com.sbqs.entity.Counter;
import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.Ticket;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.QueueMachineRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.TicketRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class QueueMachineService {

    private final QueueMachineRepository queueMachineRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final TicketRepository ticketRepository;
    private final CounterRepository counterRepository;
    private final CurrentUserService currentUserService;
    private final DomainEventPublisher eventPublisher;

    public QueueMachineService(
            QueueMachineRepository queueMachineRepository,
            QueueMachineServiceMappingRepository mappingRepository,
            TicketRepository ticketRepository,
            CounterRepository counterRepository,
            CurrentUserService currentUserService,
            DomainEventPublisher eventPublisher) {
        this.queueMachineRepository = queueMachineRepository;
        this.mappingRepository = mappingRepository;
        this.ticketRepository = ticketRepository;
        this.counterRepository = counterRepository;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    public List<QueueMachine> getAllQueueMachines() {
        return queueMachineRepository.findByBranch(currentUserService.requireUser().getBranch());
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "services", allEntries = true),
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    })
    public QueueMachine createQueueMachine(QueueMachine queueMachine) {
        currentUserService.requireBranch(queueMachine.getBranch().getBranchId());
        queueMachine.setBranch(currentUserService.requireUser().getBranch());
        if (queueMachineRepository.existsByBranchAndMachineCode(
                queueMachine.getBranch(),
                queueMachine.getMachineCode())) {
            throw new RuntimeException("Ma may boc so da ton tai trong chi nhanh nay");
        }

        QueueMachine savedQueueMachine = queueMachineRepository.save(queueMachine);
        eventPublisher.publish(
                "QUEUE_MACHINE_CREATED",
                "QUEUE_MACHINE",
                savedQueueMachine.getQueueMachineId().toString(),
                savedQueueMachine.getBranch().getBranchId(),
                Map.of(
                        "machineCode", savedQueueMachine.getMachineCode(),
                        "machineName", savedQueueMachine.getMachineName()));

        return savedQueueMachine;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "services", allEntries = true),
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    })
    public QueueMachine updateQueueMachine(
            Long queueMachineId,
            QueueMachine updatedQueueMachine) {

        QueueMachine existingQueueMachine = queueMachineRepository.findById(queueMachineId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay may boc so"));

        currentUserService.requireBranch(existingQueueMachine.getBranch().getBranchId());
        currentUserService.requireBranch(updatedQueueMachine.getBranch().getBranchId());

        if (queueMachineRepository.existsByBranchAndMachineCodeAndQueueMachineIdNot(
                updatedQueueMachine.getBranch(),
                updatedQueueMachine.getMachineCode(),
                queueMachineId)) {
            throw new RuntimeException("Ma may boc so da ton tai trong chi nhanh nay");
        }

        existingQueueMachine.setMachineCode(updatedQueueMachine.getMachineCode());
        existingQueueMachine.setMachineName(updatedQueueMachine.getMachineName());
        existingQueueMachine.setLocationNote(updatedQueueMachine.getLocationNote());
        existingQueueMachine.setInstructionNote(updatedQueueMachine.getInstructionNote());
        existingQueueMachine.setStatus(updatedQueueMachine.getStatus());
        existingQueueMachine.setBranch(updatedQueueMachine.getBranch());

        QueueMachine savedQueueMachine = queueMachineRepository.save(existingQueueMachine);
        eventPublisher.publish(
                "QUEUE_MACHINE_UPDATED",
                "QUEUE_MACHINE",
                savedQueueMachine.getQueueMachineId().toString(),
                savedQueueMachine.getBranch().getBranchId(),
                Map.of(
                        "machineCode", savedQueueMachine.getMachineCode(),
                        "machineName", savedQueueMachine.getMachineName(),
                        "status", savedQueueMachine.getStatus()));

        return savedQueueMachine;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "services", allEntries = true),
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    })
    @Transactional
    public void deleteQueueMachine(Long queueMachineId) {
        QueueMachine existingQueueMachine = queueMachineRepository.findById(queueMachineId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay may boc so"));

        currentUserService.requireBranch(existingQueueMachine.getBranch().getBranchId());

        List<Ticket> machineTickets = ticketRepository.findByQueueMachine(existingQueueMachine);
        boolean hasOpenTicket = machineTickets.stream()
                .anyMatch(ticket -> Set.of("WAITING", "SERVING").contains(ticket.getStatus()));
        if (hasOpenTicket) {
            throw new RuntimeException("Khong the xoa may vi con phieu dang cho hoac dang phuc vu");
        }

        List<Counter> counters = counterRepository.findByBranchBranchIdAndQueueMachineQueueMachineId(
                existingQueueMachine.getBranch().getBranchId(),
                existingQueueMachine.getQueueMachineId());
        boolean hasActiveCounter = counters.stream()
                .anyMatch(counter -> counter.getCurrentTicket() != null || "ACTIVE".equals(counter.getStatus()));
        if (hasActiveCounter) {
            throw new RuntimeException("Khong the xoa may vi con quay dang hoat dong tren may nay");
        }

        mappingRepository.deleteAll(mappingRepository.findByQueueMachine(existingQueueMachine));
        counters.forEach(counter -> counter.setQueueMachine(null));
        counterRepository.saveAll(counters);
        machineTickets.forEach(ticket -> ticket.setQueueMachine(null));
        ticketRepository.saveAll(machineTickets);

        queueMachineRepository.delete(existingQueueMachine);
        eventPublisher.publish(
                "QUEUE_MACHINE_DELETED",
                "QUEUE_MACHINE",
                queueMachineId.toString(),
                existingQueueMachine.getBranch().getBranchId(),
                Map.of(
                        "machineCode", existingQueueMachine.getMachineCode(),
                        "machineName", existingQueueMachine.getMachineName()));
    }
}
