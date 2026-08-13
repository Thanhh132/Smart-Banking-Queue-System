package com.sbqs.service;

import com.sbqs.dto.service.ServiceCatalogRequest;
import com.sbqs.entity.ServiceCatalog;
import com.sbqs.entity.Services;
import com.sbqs.entity.Branch;
import com.sbqs.repository.AppointmentRepository;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.DigitalDelegationRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceCatalogRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ServiceCatalogService {
    private final ServiceCatalogRepository catalogRepository;
    private final ServiceRepository serviceRepository;
    private final BranchRepository branchRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final TicketRepository ticketRepository;
    private final AppointmentRepository appointmentRepository;
    private final DigitalDelegationRepository delegationRepository;

    public ServiceCatalogService(ServiceCatalogRepository catalogRepository,
                                 ServiceRepository serviceRepository,
                                 BranchRepository branchRepository,
                                 QueueMachineServiceMappingRepository mappingRepository,
                                 TicketRepository ticketRepository,
                                 AppointmentRepository appointmentRepository,
                                 DigitalDelegationRepository delegationRepository) {
        this.catalogRepository = catalogRepository;
        this.serviceRepository = serviceRepository;
        this.branchRepository = branchRepository;
        this.mappingRepository = mappingRepository;
        this.ticketRepository = ticketRepository;
        this.appointmentRepository = appointmentRepository;
        this.delegationRepository = delegationRepository;
    }

    public List<ServiceCatalog> getCatalog() {
        return catalogRepository.findAllByOrderByServiceNameAsc();
    }

    @Transactional
    @CacheEvict(cacheNames = {"services", "queueMonitor"}, allEntries = true)
    public synchronized ServiceCatalog create(ServiceCatalogRequest request) {
        validate(request, false);
        String code = request.serviceCode() == null || request.serviceCode().isBlank()
                ? nextServiceCode()
                : request.serviceCode().trim().toUpperCase();
        String name = request.serviceName().trim();
        if (!code.matches("^[A-Z][A-Z0-9_]{1,49}$")) {
            throw new RuntimeException("Mã dịch vụ chỉ gồm chữ in hoa, số và dấu gạch dưới");
        }
        if (catalogRepository.existsByServiceCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã dịch vụ đã tồn tại trong danh mục hệ thống");
        }
        if (catalogRepository.existsByServiceNameIgnoreCase(name)) {
            throw new RuntimeException("Tên dịch vụ đã tồn tại trong danh mục hệ thống");
        }

        ServiceCatalog item = new ServiceCatalog();
        item.setServiceCode(code);
        item.setServiceName(name);
        item.setServiceType(request.serviceType().trim().toUpperCase());
        item.setDescription(request.description() == null ? null : request.description().trim());
        item.setEstimatedTime(request.estimatedTime());
        item.setDelegatable(request.delegatable());
        item.setStatus("ACTIVE");
        item.setFormSchema(new ArrayList<>());
        ServiceCatalog saved = catalogRepository.save(item);
        inheritCatalogItemForAllBranches(saved);
        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = {"services", "queueMonitor"}, allEntries = true)
    public ServiceCatalog update(Long catalogId, ServiceCatalogRequest request) {
        validate(request, true);
        ServiceCatalog item = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ trong danh mục hệ thống"));
        String code = request.serviceCode().trim().toUpperCase();
        String name = request.serviceName().trim();
        if (!item.getServiceCode().equalsIgnoreCase(code)) {
            throw new RuntimeException("Không thể đổi mã dịch vụ sau khi đã tạo");
        }
        catalogRepository.findByServiceCodeIgnoreCase(code)
                .filter(existing -> !existing.getCatalogId().equals(catalogId))
                .ifPresent(existing -> { throw new RuntimeException("Mã dịch vụ đã tồn tại trong danh mục hệ thống"); });
        catalogRepository.findAllByOrderByServiceNameAsc().stream()
                .filter(existing -> !existing.getCatalogId().equals(catalogId))
                .filter(existing -> existing.getServiceName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresent(existing -> { throw new RuntimeException("Tên dịch vụ đã tồn tại trong danh mục hệ thống"); });
        item.setServiceCode(code);
        item.setServiceName(name);
        item.setServiceType(request.serviceType().trim().toUpperCase());
        item.setDescription(request.description() == null ? null : request.description().trim());
        item.setEstimatedTime(request.estimatedTime());
        item.setDelegatable(request.delegatable());
        ServiceCatalog saved = catalogRepository.save(item);
        List<Services> branchServices = serviceRepository.findByCatalog(saved);
        branchServices.forEach(service -> {
            service.setServiceName(saved.getServiceName());
            service.setServiceType(saved.getServiceType());
            service.setDescription(saved.getDescription());
        });
        serviceRepository.saveAll(branchServices);
        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = {"services", "queueMonitor"}, allEntries = true)
    public void delete(Long catalogId) {
        ServiceCatalog item = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ trong danh mục hệ thống"));
        List<Services> branchServices = serviceRepository.findByCatalog(item);
        boolean hasActiveOperation = branchServices.stream().anyMatch(service ->
                ticketRepository.findByService(service).stream()
                        .anyMatch(ticket -> List.of("WAITING", "SERVING").contains(ticket.getStatus()))
                || appointmentRepository.findByService(service).stream()
                        .anyMatch(appointment -> "PENDING".equals(appointment.getStatus()))
                || delegationRepository.findByService(service).stream()
                        .anyMatch(delegation -> "VERIFIED".equals(delegation.getStatus())
                                || ("ACTIVE".equals(delegation.getStatus())
                                && delegation.getValidUntil().isAfter(java.time.LocalDateTime.now()))));
        if (hasActiveOperation) {
            throw new RuntimeException("Dịch vụ còn phiếu đang xử lý, lịch hẹn chờ hoặc ủy quyền còn hiệu lực. Hãy hoàn tất/hủy các hồ sơ đang hoạt động trước khi xóa");
        }
        branchServices.forEach(service -> mappingRepository.deleteAll(mappingRepository.findByService(service)));
        mappingRepository.flush();

        boolean hasBusinessHistory = branchServices.stream().anyMatch(service ->
                !ticketRepository.findByService(service).isEmpty()
                || !appointmentRepository.findByService(service).isEmpty()
                || !delegationRepository.findByService(service).isEmpty());
        if (hasBusinessHistory) {
            // Preserve branch service rows as references for old tickets and reports,
            // but detach and hide them from all operational screens.
            branchServices.forEach(service -> {
                service.setStatus("DELETED");
                service.setCatalog(null);
            });
            serviceRepository.saveAll(branchServices);
            serviceRepository.flush();
            catalogRepository.delete(item);
            return;
        }
        serviceRepository.deleteAll(branchServices);
        serviceRepository.flush();
        catalogRepository.delete(item);
    }

    @Transactional
    @CacheEvict(cacheNames = {"services", "queueMonitor"}, allEntries = true)
    public ServiceCatalog restore(Long catalogId) {
        ServiceCatalog item = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ đã lưu trữ"));
        item.setStatus("ACTIVE");
        ServiceCatalog saved = catalogRepository.save(item);
        List<Services> branchServices = serviceRepository.findByCatalog(saved);
        branchServices.forEach(service -> service.setStatus("ACTIVE"));
        serviceRepository.saveAll(branchServices);
        inheritCatalogItemForAllBranches(saved);
        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = {"services", "queueMonitor"}, allEntries = true)
    public void inheritCatalogForBranch(Branch branch) {
        catalogRepository.findAllByOrderByServiceNameAsc().stream()
                .filter(item -> "ACTIVE".equals(item.getStatus()))
                .forEach(item -> inherit(branch, item));
    }

    @Transactional
    @CacheEvict(cacheNames = {"services", "queueMonitor"}, allEntries = true)
    public void synchronizeAllBranches() {
        branchRepository.findAll().forEach(this::inheritCatalogForBranch);
    }

    private void inheritCatalogItemForAllBranches(ServiceCatalog item) {
        branchRepository.findAll().forEach(branch -> inherit(branch, item));
    }

    private Services inherit(Branch branch, ServiceCatalog item) {
        return serviceRepository.findByBranch(branch).stream()
                .filter(service -> item.getServiceCode().equalsIgnoreCase(service.getServiceCode()))
                .findFirst()
                .map(service -> {
                    service.setCatalog(item);
                    service.setServiceCode(item.getServiceCode());
                    service.setServiceName(item.getServiceName());
                    service.setServiceType(item.getServiceType());
                    service.setDescription(item.getDescription());
                    service.setEstimatedTime(item.getEstimatedTime());
                    if ("DELETED".equalsIgnoreCase(service.getStatus())) {
                        service.setStatus("ACTIVE");
                    }
                    return serviceRepository.save(service);
                })
                .orElseGet(() -> serviceRepository.save(newBranchService(branch, item)));
    }

    private Services newBranchService(Branch branch, ServiceCatalog item) {
        Services service = new Services();
        service.setCatalog(item);
        service.setBranch(branch);
        service.setServiceCode(item.getServiceCode());
        service.setServiceName(item.getServiceName());
        service.setServiceType(item.getServiceType());
        service.setDescription(item.getDescription());
        service.setEstimatedTime(item.getEstimatedTime());
        service.setStatus("ACTIVE");
        service.setRequiredCustomerFields(CustomerProfilePolicy.includeDefaults(List.of()));
        service.setFormSchema(new ArrayList<>(item.getFormSchema()));
        return service;
    }

    private String nextServiceCode() {
        long maxSequence = Stream.concat(
                        catalogRepository.findAllByOrderByServiceNameAsc().stream()
                                .map(ServiceCatalog::getServiceCode),
                        serviceRepository.findAll().stream().map(Services::getServiceCode))
                .map(this::serviceCodeSequence)
                .max(Long::compareTo)
                .orElse(0L);
        return "DV%03d".formatted(maxSequence + 1);
    }

    private long serviceCodeSequence(String code) {
        if (code == null || !code.matches("(?i)^DV\\d+$")) {
            return 0L;
        }
        try {
            return Long.parseLong(code.substring(2));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void validate(ServiceCatalogRequest request, boolean codeRequired) {
        if (request == null
                || (codeRequired && (request.serviceCode() == null || request.serviceCode().isBlank()))
                || request.serviceName() == null || request.serviceName().isBlank()
                || request.serviceType() == null || request.serviceType().isBlank()
                || request.estimatedTime() == null || request.estimatedTime() <= 0) {
            throw new RuntimeException("Tên, nhóm và thời gian dịch vụ phải hợp lệ");
        }
        if (request.serviceCode() != null && !request.serviceCode().isBlank()
                && !request.serviceCode().trim().toUpperCase().matches("^[A-Z][A-Z0-9_]{1,49}$")) {
            throw new RuntimeException("Mã dịch vụ chỉ gồm chữ in hoa, số và dấu gạch dưới");
        }
    }

}
