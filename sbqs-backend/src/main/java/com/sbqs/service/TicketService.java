package com.sbqs.service;

import com.sbqs.entity.Counter;
import com.sbqs.entity.QueueMachine;
import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.entity.User;
import com.sbqs.dto.CreatePreparedTicketRequest;
import com.sbqs.dto.TicketStaffViewResponse;
import com.sbqs.dto.TicketTrackingResponse;
import com.sbqs.event.DomainEventPublisher;
import com.sbqs.event.TicketQueueThresholdNotification;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.CounterSessionRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.QueueMachineRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final QueueMachineRepository queueMachineRepository;
    private final CounterRepository counterRepository;
    private final HistoryService historyService;
    private final BranchRepository branchRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;
    private final CounterSessionRepository counterSessionRepository;
    private final TicketWorkflowService ticketWorkflowService;
    private final DomainEventPublisher eventPublisher;
    private final PreparedTransactionService preparedTransactionService;
    private final BranchOperatingHoursService operatingHoursService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TicketService(
            TicketRepository ticketRepository,
            QueueMachineServiceMappingRepository mappingRepository,
            QueueMachineRepository queueMachineRepository,
            CounterRepository counterRepository,
            HistoryService historyService,
            BranchRepository branchRepository,
            ServiceRepository serviceRepository,
            CurrentUserService currentUserService,
            CounterSessionRepository counterSessionRepository,
            TicketWorkflowService ticketWorkflowService,
            DomainEventPublisher eventPublisher,
            PreparedTransactionService preparedTransactionService,
            BranchOperatingHoursService operatingHoursService,
            ApplicationEventPublisher applicationEventPublisher) {

        this.ticketRepository = ticketRepository;
        this.mappingRepository = mappingRepository;
        this.queueMachineRepository = queueMachineRepository;
        this.counterRepository = counterRepository;
        this.historyService = historyService;
        this.branchRepository = branchRepository;
        this.serviceRepository = serviceRepository;
        this.currentUserService = currentUserService;
        this.counterSessionRepository = counterSessionRepository;
        this.ticketWorkflowService = ticketWorkflowService;
        this.eventPublisher = eventPublisher;
        this.preparedTransactionService = preparedTransactionService;
        this.operatingHoursService = operatingHoursService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Luồng paperless: kiểm tra dữ liệu theo schema trước, sau đó cấp phiếu và lưu
     * snapshot biểu mẫu để thay đổi cấu hình dịch vụ về sau không làm sai hồ sơ cũ.
     */
    @Transactional
    public Ticket createPreparedTicket(CreatePreparedTicketRequest request) {
        Services service = serviceRepository.findById(request.serviceId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));
        Map<String, Object> sanitizedValues = preparedTransactionService.validateForm(service, request.values());

        Ticket ticket = new Ticket();
        Branch branch = new Branch();
        branch.setBranchId(request.branchId());
        ticket.setBranch(branch);
        ticket.setService(service);
        Ticket savedTicket = createTicket(ticket);

        preparedTransactionService.saveDraft(savedTicket, service, savedTicket.getCustomer(), sanitizedValues);
        return savedTicket;
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findByBranch(currentUserService.requireUser().getBranch());
    }

    @Transactional
    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    /**
     * Cấp số cho CUSTOMER: kiểm tra dịch vụ được map vào máy, chống lấy nhiều phiếu
     * đang hoạt động và tăng số thứ tự của đúng máy bốc số trong transaction.
     */
    public Ticket createTicket(Ticket ticket) {
        User customer = currentUserService.requireUser();
        if (!"CUSTOMER".equals(customer.getRole())) {
            throw new RuntimeException("Chi khach hang moi co the lay so");
        }
        List<Ticket> activeTickets = ticketRepository.findByCustomerUserIdAndStatusIn(
                customer.getUserId(), List.of("WAITING", "SERVING"));

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
        operatingHoursService.requireOpen(branch.getBranchId());
        Services service = serviceRepository.findById(ticket.getService().getServiceId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));
        preparedTransactionService.requireCompleteProfile(customer, service);

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

        QueueMachine queueMachine = queueMachineRepository
                .findByIdForTicketIssuing(mapping.getQueueMachine().getQueueMachineId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay may boc so"));
        ticket.setQueueMachine(queueMachine);

        int nextTicketNumber = queueMachine.getLastTicketNumber() + 1;
        queueMachine.setLastTicketNumber(nextTicketNumber);
        queueMachineRepository.save(queueMachine);

        ticket.setTicketNumber(nextTicketNumber);
        ticket.setStatus("WAITING");
        ticket.setCustomer(customer);

        Ticket savedTicket = ticketRepository.save(ticket);
        ticketWorkflowService.startTicketApproval(savedTicket);
        notifyIfAlreadyNearFront(savedTicket);
        eventPublisher.publish(
                "TICKET_CREATED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "customerId", savedTicket.getCustomer().getUserId(),
                        "serviceName", savedTicket.getService().getServiceName(),
                        "queueMachineName", savedTicket.getQueueMachine().getMachineName()));

        return savedTicket;
    }

    public List<Ticket> getTicketsByStatus(String status) {
        return ticketRepository.findByStatus(status);
    }

    /** Lấy phiếu đang hoạt động của chính email trong JWT, không cho xem phiếu người khác. */
    public Ticket getCurrentCustomerTicket() {
        User customer = currentUserService.requireUser();
        return ticketRepository.findFirstByCustomerUserIdAndStatusInOrderByCreatedAtDesc(
                        customer.getUserId(), List.of("WAITING", "SERVING"))
                .orElse(null);
    }

    /** Trả trạng thái realtime và số người đang chờ phía trước cho màn hình theo dõi của khách hàng. */
    public TicketTrackingResponse trackCustomerTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu"));
        User currentCustomer = currentUserService.requireUser();
        if (!ownsTicket(ticket, currentCustomer)) {
            throw new RuntimeException("Bạn không có quyền theo dõi phiếu này");
        }

        LocalDate today = LocalDate.now();
        long peopleAhead = "WAITING".equals(ticket.getStatus())
                ? ticketRepository.countWaitingAhead(
                        ticket.getQueueMachine(),
                        ticket.getTicketNumber(),
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay())
                : 0;
        String counterName = counterRepository.findFirstByCurrentTicketTicketId(ticketId)
                .map(Counter::getCounterName)
                .orElse(null);

        return new TicketTrackingResponse(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                ticket.getStatus(),
                peopleAhead,
                counterName,
                ticket.getBranch() == null ? null : ticket.getBranch().getBranchName(),
                ticket.getService() == null ? null : ticket.getService().getServiceName(),
                ticket.getQueueMachine() == null ? null : ticket.getQueueMachine().getQueueMachineId(),
                ticket.getQueueMachine() == null ? null : ticket.getQueueMachine().getLocationNote(),
                ticket.getServingStartedAt());
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    @Transactional
    /**
     * Nhân viên gọi phiếu WAITING tiếp theo phù hợp dịch vụ của quầy; đồng thời chuyển
     * phiếu sang SERVING, gắn quầy/nhân viên và cập nhật workflow Camunda.
     */
    public TicketStaffViewResponse callNextTicket(Long counterId) {
        Counter counter = counterRepository.findByIdForUpdate(counterId)
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
                        "WAITING")
                .orElseThrow(() -> new RuntimeException("Không còn khách đang chờ"));

        ticketWorkflowService.approveForServing(nextTicket, counter);

        nextTicket.setStatus("SERVING");
        nextTicket.setServingStartedAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(nextTicket);

        counter.setCurrentTicket(savedTicket);
        counterRepository.save(counter);
        notifyTicketThatReachedThreshold(counter.getQueueMachine());
        eventPublisher.publish(
                "TICKET_CALLED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "counterName", counter.getCounterName(),
                        "serviceName", savedTicket.getService().getServiceName()));

        return preparedTransactionService.toStaffView(savedTicket);
    }

    private void notifyTicketThatReachedThreshold(QueueMachine queueMachine) {
        List<Ticket> firstWaiting = ticketRepository.findByQueueMachineAndStatusOrderByTicketNumberAsc(
                queueMachine, "WAITING", PageRequest.of(0, 4));
        if (firstWaiting.size() < 4) return;

        Ticket target = firstWaiting.get(3);
        applicationEventPublisher.publishEvent(new TicketQueueThresholdNotification(
                target.getTicketId(), target.getTicketNumber(), 3));
        eventPublisher.publish(
                "TICKET_QUEUE_NEAR",
                "TICKET",
                target.getTicketId().toString(),
                target.getBranch().getBranchId(),
                Map.of("ticketNumber", target.getTicketNumber(), "peopleAhead", 3));
    }

    private void notifyIfAlreadyNearFront(Ticket ticket) {
        long peopleAhead = ticketRepository.countByQueueMachineAndStatusAndTicketNumberLessThan(
                ticket.getQueueMachine(), "WAITING", ticket.getTicketNumber());
        if (peopleAhead > 3) return;

        applicationEventPublisher.publishEvent(new TicketQueueThresholdNotification(
                ticket.getTicketId(), ticket.getTicketNumber(), peopleAhead));
    }

    @Transactional(readOnly = true)
    /** Chi nhan vien dang giu quay phuc vu moi duoc xem ho so giay to cua phieu da goi. */
    public TicketStaffViewResponse getServingTicketForStaff(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ticket"));

        if (!"SERVING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chi hien ho so giay to sau khi phieu da duoc goi");
        }

        Counter counter = counterRepository.findFirstByCurrentTicketTicketId(ticketId)
                .orElseThrow(() -> new RuntimeException("Phieu nay chua duoc goi vao quay nao"));
        requireCurrentStaffOwnsCounter(counter);

        return preparedTransactionService.toStaffView(ticket);
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    /** Hoàn tất phiếu đang phục vụ và ghi snapshot vào lịch sử để báo cáo không phụ thuộc dữ liệu sau này. */
    public Ticket completeTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ticket"));

        if (!"SERVING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chi ticket dang phuc vu moi duoc hoan thanh");
        }

        Counter counter = counterRepository.findFirstByCurrentTicketTicketId(ticketId).orElse(null);

        if (counter == null) {
            throw new RuntimeException("Ticket khong duoc phuc vu tai quay nao");
        }

        requireCurrentStaffOwnsCounter(counter);
        User currentStaff = currentUserService.requireUser();
        ticketWorkflowService.completeServing(ticket);

        ticket.setStatus("COMPLETED");

        historyService.recordCompleted(ticket, counter, currentStaff);

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
    @Transactional
    public Ticket markCustomerNoShow(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu"));
        if (!"SERVING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chỉ có thể bỏ qua phiếu đang được gọi");
        }

        Counter counter = counterRepository.findFirstByCurrentTicketTicketId(ticketId)
                .orElseThrow(() -> new RuntimeException("Phiếu không thuộc quầy đang phục vụ"));
        requireCurrentStaffOwnsCounter(counter);
        User currentStaff = currentUserService.requireUser();

        ticketWorkflowService.closeNoShow(ticket);
        ticket.setStatus("MISSED");
        historyService.recordMissed(ticket, counter, currentStaff);
        counter.setCurrentTicket(null);
        counterRepository.save(counter);

        Ticket savedTicket = ticketRepository.save(ticket);
        eventPublisher.publish(
                "TICKET_MISSED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "staffName", currentStaff.getFullName(),
                        "counterName", counter.getCounterName()));
        return savedTicket;
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    /** Chỉ CUSTOMER sở hữu phiếu mới được hủy phiếu chưa hoàn tất của mình. */
    public Ticket cancelTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ticket"));

        User currentCustomer = currentUserService.requireUser();
        if (!ownsTicket(ticket, currentCustomer)) {
            throw new RuntimeException("Ban khong co quyen huy ticket nay");
        }

        if (!"WAITING".equals(ticket.getStatus())) {
            throw new RuntimeException("Chi ticket dang cho moi duoc huy");
        }

        ticket.setStatus("CANCELLED");
        ticketWorkflowService.cancelTicket(ticket);

        Ticket savedTicket = ticketRepository.save(ticket);
        historyService.recordCancelled(savedTicket);

        eventPublisher.publish(
                "TICKET_CANCELLED",
                "TICKET",
                savedTicket.getTicketId().toString(),
                savedTicket.getBranch().getBranchId(),
                Map.of(
                        "ticketNumber", savedTicket.getTicketNumber(),
                        "customerId", savedTicket.getCustomer().getUserId(),
                        "serviceName", savedTicket.getService().getServiceName()));

        return savedTicket;
    }

    /** Missing customer_id fails closed; email is never an authorization key. */
    private boolean ownsTicket(Ticket ticket, User customer) {
        return ticket.getCustomer() != null
                && ticket.getCustomer().getUserId().equals(customer.getUserId());
    }

    /** Bao dam nhan vien chi thao tac tren quay dang duoc chinh minh nhan trong ca hien tai. */
    private void requireCurrentStaffOwnsCounter(Counter counter) {
        User currentStaff = currentUserService.requireUser();
        counterSessionRepository
                .findFirstByCounterIdAndStatusOrderByStartedAtDesc(counter.getCounterId(), "ACTIVE")
                .filter(session -> session.getStaffId().equals(currentStaff.getUserId()))
                .orElseThrow(() -> new RuntimeException(
                        "Ban phai assign vao quay nay truoc khi thao tac ticket"));
    }
}
