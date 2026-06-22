package com.sbqs.service;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Counter;
import com.sbqs.entity.CounterSession;
import com.sbqs.entity.User;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.CounterSessionRepository;
import com.sbqs.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CounterService {

    private final CounterRepository counterRepository;
    private final CounterSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public CounterService(
            CounterRepository counterRepository,
            CounterSessionRepository sessionRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {

        this.counterRepository = counterRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
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
                .findFirstByStaffAndStatusOrderByStartedAtDesc(staff, "ACTIVE")
                .map(CounterSession::getCounter)
                .orElse(null);
    }

    public Counter createCounter(Counter counter) {
        currentUserService.requireBranch(counter.getBranch().getBranchId());
        counter.setBranch(currentUserService.requireUser().getBranch());
        if (counterRepository.existsByBranchAndCounterCode(
                counter.getBranch(),
                counter.getCounterCode())) {
            throw new RuntimeException("Ma quay da ton tai trong chi nhanh nay");
        }

        counter.setStatus("INACTIVE");
        return counterRepository.save(counter);
    }

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
        counter.setBranch(request.getBranch());
        counter.setQueueMachine(request.getQueueMachine());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            counter.setStatus(request.getStatus());
        }

        return counterRepository.save(counter);
    }

    public Counter assignCounter(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay quay"));
        User staff = getCurrentUser();

        if (staff.getBranch() == null
                || !staff.getBranch().getBranchId().equals(counter.getBranch().getBranchId())) {
            throw new RuntimeException("Nhan vien khong thuoc chi nhanh cua quay nay");
        }

        sessionRepository.findFirstByStaffAndStatusOrderByStartedAtDesc(staff, "ACTIVE")
                .ifPresent(session -> {
                    throw new RuntimeException("Nhan vien dang assign vao quay khac. Hay unassign truoc.");
                });

        sessionRepository.findFirstByCounterAndStatusOrderByStartedAtDesc(counter, "ACTIVE")
                .ifPresent(session -> {
                    throw new RuntimeException("Quay nay dang co nhan vien phuc vu.");
                });

        CounterSession session = new CounterSession();
        session.setCounter(counter);
        session.setStaff(staff);
        session.setBranch(counter.getBranch());
        session.setStartedAt(LocalDateTime.now());
        session.setStatus("ACTIVE");
        sessionRepository.save(session);

        counter.setStatus("ACTIVE");
        return counterRepository.save(counter);
    }

    public Counter unassignCounter(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay quay"));
        User staff = getCurrentUser();

        CounterSession session = sessionRepository
                .findFirstByCounterAndStatusOrderByStartedAtDesc(counter, "ACTIVE")
                .orElseThrow(() -> new RuntimeException("Quay nay chua duoc assign"));

        if (!session.getStaff().getUserId().equals(staff.getUserId())) {
            throw new RuntimeException("Chi nhan vien dang assign moi duoc unassign quay nay");
        }

        if (counter.getCurrentTicket() != null) {
            throw new RuntimeException("Hay hoan thanh ticket hien tai truoc khi unassign");
        }

        session.setEndedAt(LocalDateTime.now());
        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        counter.setStatus("INACTIVE");
        return counterRepository.save(counter);
    }

    public void deleteCounter(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay quay"));

        currentUserService.requireBranch(counter.getBranch().getBranchId());

        counterRepository.delete(counter);
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
