package com.sbqs.service;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Counter;
import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.entity.User;
import com.sbqs.repository.AppointmentRepository;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.CounterRepository;
import com.sbqs.repository.HistoryRepository;
import com.sbqs.repository.QueueMachineRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import com.sbqs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final HistoryRepository historyRepository;
    private final AppointmentRepository appointmentRepository;
    private final TicketRepository ticketRepository;
    private final CounterRepository counterRepository;
    private final QueueMachineRepository queueMachineRepository;
    private final ServiceRepository serviceRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;

    public BranchService(
            BranchRepository branchRepository,
            UserRepository userRepository,
            KeycloakService keycloakService,
            HistoryRepository historyRepository,
            AppointmentRepository appointmentRepository,
            TicketRepository ticketRepository,
            CounterRepository counterRepository,
            QueueMachineRepository queueMachineRepository,
            ServiceRepository serviceRepository,
            QueueMachineServiceMappingRepository mappingRepository) {

        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
        this.keycloakService = keycloakService;
        this.historyRepository = historyRepository;
        this.appointmentRepository = appointmentRepository;
        this.ticketRepository = ticketRepository;
        this.counterRepository = counterRepository;
        this.queueMachineRepository = queueMachineRepository;
        this.serviceRepository = serviceRepository;
        this.mappingRepository = mappingRepository;
    }

    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    public List<Branch> getBranchesByBank(String bankName) {
        return branchRepository.findByBankName(bankName);
    }

    public List<Branch> getNearestBranches(String bankName, double latitude, double longitude) {
        return branchRepository.findNearestBranches(latitude, longitude, bankName);
    }

    public Branch createBranch(Branch branch) {
        if (branch.getBranchCode() == null
                || branch.getBranchCode().isBlank()
                || branchRepository.existsByBranchCode(branch.getBranchCode())) {
            branch.setBranchCode(generateBranchCode(branch));
        }

        return branchRepository.save(branch);
    }

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
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            branch.setStatus(request.getStatus());
        }

        return branchRepository.save(branch);
    }

    @Transactional
    public void deleteBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay chi nhanh"));

        List<User> users = userRepository.findByBranch(branch);
        for (User user : users) {
            keycloakService.deleteUser(user.getKeycloakUserId());
        }
        userRepository.deleteAll(users);

        historyRepository.deleteAll(historyRepository.findByBranchBranchId(branchId));
        appointmentRepository.deleteAll(appointmentRepository.findByBranch(branch));

        List<QueueMachineServiceMapping> mappings = mappingRepository.findAll()
                .stream()
                .filter(mapping -> mapping.getQueueMachine().getBranch().getBranchId().equals(branchId)
                        || mapping.getService().getBranch().getBranchId().equals(branchId))
                .toList();
        mappingRepository.deleteAll(mappings);

        List<Counter> counters = counterRepository.findByBranch(branch);
        for (Counter counter : counters) {
            counter.setCurrentTicket(null);
        }
        counterRepository.saveAll(counters);

        ticketRepository.deleteAll(ticketRepository.findByBranch(branch));
        counterRepository.deleteAll(counters);
        serviceRepository.deleteAll(serviceRepository.findByBranch(branch));
        queueMachineRepository.deleteAll(queueMachineRepository.findByBranch(branch));
        branchRepository.delete(branch);
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
