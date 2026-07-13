package com.sbqs.service;

import com.sbqs.event.DomainEventPublisher;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Counter;
import com.sbqs.entity.CounterSession;
import com.sbqs.entity.User;
import com.sbqs.entity.QueueMachine;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.CounterSessionRepository;
import com.sbqs.repository.UserRepository;
import com.sbqs.repository.QueueMachineRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CounterService {

    private final CounterRepository counterRepository;
    private final CounterSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final QueueMachineRepository queueMachineRepository;
    private final CurrentUserService currentUserService;
    private final DomainEventPublisher eventPublisher;

    public CounterService(
            CounterRepository counterRepository,
            CounterSessionRepository sessionRepository,
            UserRepository userRepository,
            QueueMachineRepository queueMachineRepository,
            CurrentUserService currentUserService,
            DomainEventPublisher eventPublisher) {

        this.counterRepository = counterRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.queueMachineRepository = queueMachineRepository;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    public List<Counter> getAllCounters() {
        return counterRepository.findByBranch(currentUserService.requireUser().getBranch());
    }

    public List<Counter> getCountersByBranch(Branch branch) {
        currentUserService.requireBranch(branch.getBranchId());
        return counterRepository.findByBranch(branch);
    }

    public Counter getAssignedCounterForCurrentStaff() {
        User staff = getCurrentUser();

        return sessionRepository
                .findFirstByStaffIdAndStatusOrderByStartedAtDesc(staff.getUserId(), "ACTIVE")
                .flatMap(session -> counterRepository.findById(session.getCounterId()))
                .orElse(null);
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    public Counter createCounter(Counter counter) {
        currentUserService.requireBranch(counter.getBranch().getBranchId());
        counter.setBranch(currentUserService.requireUser().getBranch());
        counter.setQueueMachine(resolveQueueMachine(counter.getQueueMachine(), counter.getBranch().getBranchId()));
        if (counterRepository.existsByBranchAndCounterCode(
                counter.getBranch(),
                counter.getCounterCode())) {
            throw new RuntimeException("Ma quay da ton tai trong chi nhanh nay");
        }

        counter.setStatus("INACTIVE");
        Counter savedCounter = counterRepository.save(counter);
        eventPublisher.publish(
                "COUNTER_CREATED",
                "COUNTER",
                savedCounter.getCounterId().toString(),
                savedCounter.getBranch().getBranchId(),
                Map.of(
                        "counterCode", savedCounter.getCounterCode(),
                        "counterName", savedCounter.getCounterName()));

        return savedCounter;
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    public Counter updateCounter(Long counterId, Counter request) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay quay"));

        currentUserService.requireBranch(counter.getBranch().getBranchId());
        currentUserService.requireBranch(request.getBranch().getBranchId());

        if (counterRepository.existsByBranchAndCounterCodeAndCounterIdNot(
                request.getBranch(),
                request.getCounterCode(),
                counterId)) {
            throw new RuntimeException("Ma quay da ton tai trong chi nhanh nay");
        }

        counter.setCounterCode(request.getCounterCode());
        counter.setCounterName(request.getCounterName());
        counter.setBranch(currentUserService.requireUser().getBranch());
        counter.setQueueMachine(resolveQueueMachine(request.getQueueMachine(), counter.getBranch().getBranchId()));

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            counter.setStatus(request.getStatus());
        }

        Counter savedCounter = counterRepository.save(counter);
        eventPublisher.publish(
                "COUNTER_UPDATED",
                "COUNTER",
                savedCounter.getCounterId().toString(),
                savedCounter.getBranch().getBranchId(),
                Map.of(
                        "counterCode", savedCounter.getCounterCode(),
                        "counterName", savedCounter.getCounterName(),
                        "status", savedCounter.getStatus()));

        return savedCounter;
    }

    private QueueMachine resolveQueueMachine(QueueMachine requestedMachine, Long branchId) {
        if (requestedMachine == null) return null;
        if (requestedMachine.getQueueMachineId() == null) {
            throw new RuntimeException("Chua chon may boc so");
        }
        QueueMachine machine = queueMachineRepository.findById(requestedMachine.getQueueMachineId())
                .orElseThrow(() -> new RuntimeException("Khong tim thay may boc so"));
        if (machine.getBranch() == null || !branchId.equals(machine.getBranch().getBranchId())) {
            throw new RuntimeException("May boc so va quay phai thuoc cung chi nhanh");
        }
        return machine;
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    /**
     * Mở ca làm việc: khóa quyền sở hữu quầy cho nhân viên hiện tại và tạo CounterSession
     * để giữ lịch sử ca kể cả sau này quầy hoặc tên nhân viên thay đổi.
     */
    public Counter assignCounter(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay quay"));
        User staff = getCurrentUser();

        if (staff.getBranch() == null
                || !staff.getBranch().getBranchId().equals(counter.getBranch().getBranchId())) {
            throw new RuntimeException("Nhan vien khong thuoc chi nhanh cua quay nay");
        }

        sessionRepository.findFirstByStaffIdAndStatusOrderByStartedAtDesc(staff.getUserId(), "ACTIVE")
                .ifPresent(session -> {
                    throw new RuntimeException("Nhan vien dang assign vao quay khac. Hay unassign truoc.");
                });

        sessionRepository.findFirstByCounterIdAndStatusOrderByStartedAtDesc(counter.getCounterId(), "ACTIVE")
                .ifPresent(session -> {
                    throw new RuntimeException("Quay nay dang co nhan vien phuc vu.");
                });

        CounterSession session = new CounterSession();
        session.setCounterId(counter.getCounterId());
        session.setCounterName(counter.getCounterName());
        session.setStaffId(staff.getUserId());
        session.setStaffName(staff.getFullName());
        session.setStaffEmail(staff.getEmail());
        session.setBranchId(counter.getBranch().getBranchId());
        session.setBranchName(counter.getBranch().getBranchName());
        session.setStartedAt(LocalDateTime.now());
        session.setStatus("ACTIVE");
        sessionRepository.save(session);

        counter.setStatus("ACTIVE");
        Counter savedCounter = counterRepository.save(counter);
        eventPublisher.publish(
                "COUNTER_ASSIGNED",
                "COUNTER",
                savedCounter.getCounterId().toString(),
                savedCounter.getBranch().getBranchId(),
                Map.of(
                        "counterName", savedCounter.getCounterName(),
                        "staffEmail", staff.getEmail(),
                        "startedAt", session.getStartedAt().toString()));

        return savedCounter;
    }

    @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    /** Kết thúc ca, giải phóng quầy và đóng CounterSession đang ACTIVE. */
    public Counter unassignCounter(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay quay"));
        User staff = getCurrentUser();

        CounterSession session = sessionRepository
                .findFirstByCounterIdAndStatusOrderByStartedAtDesc(counter.getCounterId(), "ACTIVE")
                .orElseThrow(() -> new RuntimeException("Quay nay chua duoc assign"));

        if (!session.getStaffId().equals(staff.getUserId())) {
            throw new RuntimeException("Chi nhan vien dang assign moi duoc unassign quay nay");
        }

        if (counter.getCurrentTicket() != null) {
            throw new RuntimeException("Hay hoan thanh ticket hien tai truoc khi unassign");
        }

        session.setEndedAt(LocalDateTime.now());
        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        counter.setStatus("INACTIVE");
        Counter savedCounter = counterRepository.save(counter);
        eventPublisher.publish(
                "COUNTER_UNASSIGNED",
                "COUNTER",
                savedCounter.getCounterId().toString(),
                savedCounter.getBranch().getBranchId(),
                Map.of(
                        "counterName", savedCounter.getCounterName(),
                        "staffEmail", staff.getEmail(),
                        "endedAt", session.getEndedAt().toString()));

        return savedCounter;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true),
            @CacheEvict(cacheNames = "services", allEntries = true)
    })
    /** Chỉ xóa quầy chưa phát sinh dữ liệu; dữ liệu có lịch sử nên chuyển INACTIVE thay vì xóa cứng. */
    public void deleteCounter(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay quay"));

        currentUserService.requireBranch(counter.getBranch().getBranchId());

        if (counter.getCurrentTicket() != null) {
            throw new RuntimeException("Khong the xoa quay vi dang phuc vu ticket hien tai");
        }

        sessionRepository.findFirstByCounterIdAndStatusOrderByStartedAtDesc(counterId, "ACTIVE")
                .ifPresent(session -> {
                    throw new RuntimeException("Khong the xoa quay vi dang co nhan vien trong ca lam");
                });

        counterRepository.delete(counter);
        eventPublisher.publish(
                "COUNTER_DELETED",
                "COUNTER",
                counterId.toString(),
                counter.getBranch().getBranchId(),
                Map.of("counterCode", counter.getCounterCode(), "counterName", counter.getCounterName()));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new RuntimeException("Khong xac dinh duoc tai khoan dang dang nhap");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Khong tim thay user dang dang nhap"));
    }
}
