package com.sbqs.service;

import com.sbqs.event.DomainEventPublisher;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Counter;
import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.User;
import com.sbqs.repository.AppointmentRepository;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.DigitalDelegationRepository;
import com.sbqs.repository.QueueMachineRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import com.sbqs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final TicketRepository ticketRepository;
    private final CounterRepository counterRepository;
    private final QueueMachineRepository queueMachineRepository;
    private final ServiceRepository serviceRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final DigitalDelegationRepository delegationRepository;
    private final DomainEventPublisher eventPublisher;
    private final ServiceCatalogService serviceCatalogService;

    public BranchService(
            BranchRepository branchRepository,
            UserRepository userRepository,
            AppointmentRepository appointmentRepository,
            TicketRepository ticketRepository,
            CounterRepository counterRepository,
            QueueMachineRepository queueMachineRepository,
            ServiceRepository serviceRepository,
            QueueMachineServiceMappingRepository mappingRepository,
            DigitalDelegationRepository delegationRepository,
            DomainEventPublisher eventPublisher,
            ServiceCatalogService serviceCatalogService) {

        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.ticketRepository = ticketRepository;
        this.counterRepository = counterRepository;
        this.queueMachineRepository = queueMachineRepository;
        this.serviceRepository = serviceRepository;
        this.mappingRepository = mappingRepository;
        this.delegationRepository = delegationRepository;
        this.eventPublisher = eventPublisher;
        this.serviceCatalogService = serviceCatalogService;
    }

    @Cacheable(cacheNames = "branches", key = "'all'")
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    @Cacheable(cacheNames = "branches", key = "'bank:' + #bankName")
    public List<Branch> getBranchesByBank(String bankName) {
        return branchRepository.findByBankName(bankName);
    }

    @Cacheable(
            cacheNames = "branches",
            key = "'nearest:' + #bankName + ':' + #latitude + ':' + #longitude")
    /** Sắp xếp chi nhánh theo khoảng cách từ tọa độ khách hàng để hỗ trợ chọn nơi giao dịch gần nhất. */
    public List<Branch> getNearestBranches(String bankName, double latitude, double longitude) {
        return branchRepository.findNearestBranches(latitude, longitude, bankName);
    }

    @CacheEvict(cacheNames = "branches", allEntries = true)
    @Transactional
    /** Tạo chi nhánh, chuẩn hóa địa chỉ/mã và geocode tọa độ phục vụ tìm kiếm gần nhất. */
    public Branch createBranch(Branch branch) {
        if (branch.getBranchCode() == null
                || branch.getBranchCode().isBlank()
                || branchRepository.existsByBranchCode(branch.getBranchCode())) {
            branch.setBranchCode(generateBranchCode(branch));
        }

        Branch savedBranch = branchRepository.save(branch);
        serviceCatalogService.inheritCatalogForBranch(savedBranch);
        eventPublisher.publish(
                "BRANCH_CREATED",
                "BRANCH",
                savedBranch.getBranchId().toString(),
                savedBranch.getBranchId(),
                Map.of(
                        "branchCode", savedBranch.getBranchCode(),
                        "branchName", savedBranch.getBranchName(),
                        "bankName", savedBranch.getBankName()));

        return savedBranch;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "branches", allEntries = true),
            @CacheEvict(cacheNames = "services", allEntries = true),
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    })
    public Branch updateBranch(Long branchId, Branch request) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));

        branch.setBankName(request.getBankName());
        if (request.getBranchCode() != null && !request.getBranchCode().isBlank()) {
            if (branchRepository.existsByBranchCodeAndBranchIdNot(request.getBranchCode(), branchId)) {
                throw new RuntimeException("Ma chi nhanh da ton tai");
            }
            branch.setBranchCode(request.getBranchCode());
        }
        branch.setBranchName(request.getBranchName());
        branch.setProvince(request.getProvince());
        branch.setDistrict(request.getDistrict());
        branch.setWard(request.getWard());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            branch.setStatus(request.getStatus());
        }

        Branch savedBranch = branchRepository.save(branch);
        eventPublisher.publish(
                "BRANCH_UPDATED",
                "BRANCH",
                savedBranch.getBranchId().toString(),
                savedBranch.getBranchId(),
                Map.of(
                        "branchCode", savedBranch.getBranchCode(),
                        "branchName", savedBranch.getBranchName(),
                        "status", savedBranch.getStatus()));

        return savedBranch;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "branches", allEntries = true),
            @CacheEvict(cacheNames = "services", allEntries = true),
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    })
    /** Chỉ xóa khi chưa có dữ liệu nghiệp vụ phụ thuộc; lịch sử phát sinh phải được giữ lại. */
    public void deleteBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));

        List<User> users = userRepository.findByBranch(branch);
        List<User> blockingUsers = users.stream()
                .filter(user -> !"INACTIVE".equalsIgnoreCase(user.getStatus())
                        && !"DELETED".equalsIgnoreCase(user.getStatus()))
                .toList();
        if (!blockingUsers.isEmpty()) {
            throw new RuntimeException(
                    "Khong the xoa chi nhanh vi con "
                            + blockingUsers.size()
                            + " tai khoan dang hoat dong. Hay xoa hoac chuyen tai khoan truoc.");
        }

        // Tai khoan da khoa/xoa mem van duoc giu lai cho audit, nhung phai hoan tat
        // trang thai xoa va bo khoa ngoai den chi nhanh dang bi xoa vat ly.
        users.forEach(user -> {
            user.setStatus("DELETED");
            user.setBranch(null);
        });
        userRepository.saveAll(users);
        userRepository.flush();

        var delegations = delegationRepository.findByBranch(branch);
        delegations.forEach(delegation -> {
            if (delegation.getBranchNameSnapshot() == null) {
                delegation.setBranchNameSnapshot(branch.getBranchName());
            }
            if (delegation.getServiceNameSnapshot() == null && delegation.getService() != null) {
                delegation.setServiceNameSnapshot(delegation.getService().getServiceName());
            }
            if ("ACTIVE".equalsIgnoreCase(delegation.getStatus())
                    || "VERIFIED".equalsIgnoreCase(delegation.getStatus())) {
                delegation.setStatus("CANCELLED");
            }
            delegation.setBranch(null);
            delegation.setService(null);
        });
        delegationRepository.saveAll(delegations);
        delegationRepository.flush();

        List<QueueMachineServiceMapping> mappings = mappingRepository.findAllRelatedToBranch(branchId);
        mappingRepository.deleteAll(mappings);

        List<Counter> counters = counterRepository.findByBranch(branch);
        for (Counter counter : counters) {
            counter.setCurrentTicket(null);
        }
        counterRepository.saveAll(counters);

        ticketRepository.deleteAll(ticketRepository.findByBranch(branch));
        ticketRepository.flush();
        appointmentRepository.deleteAll(appointmentRepository.findByBranch(branch));
        counterRepository.deleteAll(counters);
        serviceRepository.deleteAll(serviceRepository.findByBranch(branch));
        queueMachineRepository.deleteAll(queueMachineRepository.findByBranch(branch));
        branchRepository.delete(branch);

        eventPublisher.publish(
                "BRANCH_DELETED",
                "BRANCH",
                branchId.toString(),
                branchId,
                Map.of("branchCode", branch.getBranchCode(), "branchName", branch.getBranchName()));
    }

    private String generateBranchCode(Branch branch) {
        String bankCode = normalizeCode(branch.getBankName());
        String districtCode = normalizeCode(branch.getDistrict());

        if (bankCode.isBlank()) {
            bankCode = "BANK";
        }

        if (districtCode.isBlank()) {
            districtCode = "BR";
        }

        long nextNumber = branchRepository.count() + 1;
        return "%s-%s-%03d".formatted(bankCode, districtCode, nextNumber);
    }

    private String normalizeCode(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);

        return normalized.length() <= 6 ? normalized : normalized.substring(0, 6);
    }
}
