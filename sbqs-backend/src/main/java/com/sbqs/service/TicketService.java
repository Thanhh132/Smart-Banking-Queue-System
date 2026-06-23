package com.sbqs.service;

import com.sbqs.entity.Counter;
import com.sbqs.entity.History;
import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.event.DomainEventPublisher;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.CounterSessionRepository;
import com.sbqs.repository.HistoryRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final CounterRepository counterRepository;
    private final HistoryRepository historyRepository;
    private final BranchRepository branchRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;
    private final CounterSessionRepository counterSessionRepository;
    private final TicketWorkflowService ticketWorkflowService;
    private final DomainEventPublisher eventPublisher;

    public TicketService(
            TicketRepository ticketRepository,
            QueueMachineServiceMappingRepository mappingRepository,
            CounterRepository counterRepository,
            HistoryRepository historyRepository,
            BranchRepository branchRepository,
            ServiceRepository serviceRepository,
            CurrentUserService currentUserService,
            CounterSessionRepository counterSessionRepository,
            TicketWorkflowService ticketWorkflowService,
            DomainEventPublisher eventPublisher) {

        this.ticketRepository = ticketRepository;
        this.mappingRepository = mappingRepository;
        this.counterRepository = counterRepository;
        this.historyRepository = historyRepository;
        this.branchRepository = branchRepository;
        this.serviceRepository = serviceRepository;
        this.currentUserService = currentUserService;
        this.counterSessionRepository = counterSessionRepository;
        this.ticketWorkflowService = ticketWorkflowService;
        this.eventPublisher = eventPublisher;
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findByBranch(currentUserService.requireUser().getBranch());
    }

    @Transactional
    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    public Ticket createTicket(Ticket ticket) {
        String customerEmail = getCurrentEmail();
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new RuntimeException("Khong xac dinh duoc khach hang dang dang nhap");
        }

        List<Ticket> activeTickets = ticketRepository.findByCustomerEmailAndStatusIn(
                customerEmail,
                List.of("WAITING", "SERVING"));

        if (!activeTickets.isEmpty()) {
            throw new RuntimeException("Ban dang co ticket chua hoan thanh. Hay cho hoan thanh hoac huy ticket truoc.");
        }

        if (ticket.getBranch() == null || ticket.getBranch().getBranchId() == null) {
            throw new RuntimeException("Chua chon chi nhanh");
        }

        if (ticket.getService() == null || ticket.getService().getServiceId() == null) {
            throw new RuntimeException("Chua chon dich vu");
        }

        Branch branch = branchRepository.findById(ticket.getBranch().getBranchId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));
        Services service = serviceRepository.findById(ticket.getService().getServiceId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));

        if (service.getBranch() == null
                || !service.getBranch().getBranchId().equals(branch.getBranchId())) {
            throw new RuntimeException("Dich vu khong thuoc chi nhanh da chon");
        }

        ticket.setBranch(branch);
        ticket.setService(service);

        QueueMachineServiceMapping mapping = mappingRepository
                .findFirstByQueueMachineBranchAndService(
                        branch,
                        service)
                .orElseThrow(() -> new RuntimeException(
                        "Dich vu nay chua duoc cau hinh cho may boc so cua chi nhanh"));

        QueueMachine queueMachine = mapping.getQueueMachine();
        ticket.setQueueMachine(queueMachine);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

        ticketRepository.lockQueueMachineTicketSequence(queueMachine.getQueueMachineId());
        Integer lastTicketNumber = ticketRepository
                .findMaxTicketNumberByQueueMachineAndCreatedAtBetween(
                        queueMachine.getQueueMachineId(),
                        startOfDay,
                        startOfNextDay);

        int nextTicketNumber = lastTicketNumber + 1;

        ticket.setTicketNumber(nextTicketNumber);
        ticket.setStatus("WAITING");
        ticket.setCustomerEmail(customerEmail);

        Ticket savedTicket = ticketRepository.save(ticket);
        ticketWorkflowService.startTicketApproval(savedTicket);
        eventPublisher.publish(
                "TICKET_CREATED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "customerEmail", savedTicket.getCustomerEmail(),
                        "serviceName", savedTicket.getService().getServiceName(),
                        "queueMachineName", savedTicket.getQueueMachine().getMachineName()));

        return savedTicket;
    }

    public List<Ticket> getTicketsByStatus(String status) {
        return ticketRepository.findByStatus(status);
    }

    public Ticket getCurrentCustomerTicket() {
        String customerEmail = getCurrentEmail();
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new RuntimeException("Khong xac dinh duoc khach hang dang dang nhap");
        }

        return ticketRepository
                .findFirstByCustomerEmailAndStatusInOrderByCreatedAtDesc(
                        customerEmail,
                        List.of("WAITING", "SERVING"))
                .orElse(null);
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    public Ticket callNextTicket(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay quay"));

        requireCurrentStaffOwnsCounter(counter);

        if (counter.getCurrentTicket() != null
                && "SERVING".equals(counter.getCurrentTicket().getStatus())) {
            throw new RuntimeException("Quay dang phuc vu khach, hay hoan thanh truoc khi goi so moi");
        }

        if (counter.getQueueMachine() == null) {
            throw new RuntimeException("Quay chua duoc gan may boc so");
        }

        if (!"ACTIVE".equalsIgnoreCase(counter.getStatus())) {
            throw new RuntimeException("Quay chua duoc nhan vien assign nen chua the goi so");
        }

        Ticket nextTicket = ticketRepository
                .findFirstByQueueMachineAndStatusOrderByTicketNumberAsc(
                        counter.getQueueMachine(),
                        "WAITING");

        if (nextTicket == null) {
            throw new RuntimeException("Khong con khach dang cho");
        }

        ticketWorkflowService.approveForServing(nextTicket, counter);

        nextTicket.setStatus("SERVING");
        nextTicket.setServingStartedAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(nextTicket);

        counter.setCurrentTicket(savedTicket);
        counterRepository.save(counter);
        eventPublisher.publish(
                "TICKET_CALLED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "counterName", counter.getCounterName(),
                        "serviceName", savedTicket.getService().getServiceName()));

        return savedTicket;
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    public Ticket completeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ticket"));

        if (!"SERVING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chi ticket dang phuc vu moi duoc hoan thanh");
        }

        Counter counter = counterRepository.findAll()
                .stream()
                .filter(c -> c.getCurrentTicket() != null
                        && c.getCurrentTicket().getTicketId().equals(ticketId))
                .findFirst()
                .orElse(null);

        if (counter == null) {
            throw new RuntimeException("Ticket khong duoc phuc vu tai quay nao");
        }

        requireCurrentStaffOwnsCounter(counter);
        ticketWorkflowService.completeServing(ticket);

        ticket.setStatus("COMPLETED");

        History history = new History();
        history.setTicket(ticket);
        history.setBranch(ticket.getBranch());
        history.setQueueMachine(ticket.getQueueMachine());
        history.setCounter(counter);
        history.setService(ticket.getService());
        history.setTicketNumber(ticket.getTicketNumber());
        history.setStartedAt(ticket.getServingStartedAt());
        history.setCompletedAt(LocalDateTime.now());
        history.setStaffNote("Hoan thanh phuc vu khach hang");

        historyRepository.save(history);

        if (counter != null) {
            counter.setCurrentTicket(null);
            counterRepository.save(counter);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        eventPublisher.publish(
                "TICKET_COMPLETED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "counterName", counter.getCounterName(),
                        "serviceName", savedTicket.getService().getServiceName()));

        return savedTicket;
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    public Ticket cancelTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ticket"));

        String customerEmail = getCurrentEmail();
        if (ticket.getCustomerEmail() != null
                && customerEmail != null
                && !ticket.getCustomerEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Ban khong co quyen huy ticket nay");
        }

        if (!"WAITING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chi ticket dang cho moi duoc huy");
        }

        ticket.setStatus("CANCELLED");
        ticketWorkflowService.cancelTicket(ticket);

        Ticket savedTicket = ticketRepository.save(ticket);
        eventPublisher.publish(
                "TICKET_CANCELLED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "customerEmail", savedTicket.getCustomerEmail(),
                        "serviceName", savedTicket.getService().getServiceName()));

        return savedTicket;
    }

    private String getCurrentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        return email;
    }

    private void requireCurrentStaffOwnsCounter(Counter counter) {
        User currentStaff = currentUserService.requireUser();
        counterSessionRepository
                .findFirstByCounterAndStatusOrderByStartedAtDesc(counter, "ACTIVE")
                .filter(session -> session.getStaff().getUserId().equals(currentStaff.getUserId()))
                .orElseThrow(() -> new RuntimeException(
                        "Ban phai assign vao quay nay truoc khi thao tac ticket"));
    }
}
