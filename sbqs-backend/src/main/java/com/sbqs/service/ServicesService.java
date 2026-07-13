package com.sbqs.service;

import com.sbqs.event.DomainEventPublisher;
import com.sbqs.dto.service.ServiceRequest;
import com.sbqs.entity.Appointment;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.repository.AppointmentRepository;
import com.sbqs.repository.BranchRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import com.sbqs.mapper.ServiceDtoMapper;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import com.sbqs.entity.FormFieldDefinition;

@Service
public class ServicesService {

    private final ServiceRepository serviceRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final TicketRepository ticketRepository;
    private final AppointmentRepository appointmentRepository;
    private final BranchRepository branchRepository;
    private final ServiceDtoMapper serviceDtoMapper;
    private final CurrentUserService currentUserService;
    private final DomainEventPublisher eventPublisher;

    public ServicesService(
            ServiceRepository serviceRepository,
            QueueMachineServiceMappingRepository mappingRepository,
            TicketRepository ticketRepository,
            AppointmentRepository appointmentRepository,
            BranchRepository branchRepository,
            ServiceDtoMapper serviceDtoMapper,
            CurrentUserService currentUserService,
            DomainEventPublisher eventPublisher) {

        this.serviceRepository = serviceRepository;
        this.mappingRepository = mappingRepository;
        this.ticketRepository = ticketRepository;
        this.appointmentRepository = appointmentRepository;
        this.branchRepository = branchRepository;
        this.serviceDtoMapper = serviceDtoMapper;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    public List<Services> getAllServices() {
        return serviceRepository.findByBranch(currentUserService.requireUser().getBranch());
    }

    public List<Services> getServices(Long branchId, String serviceType, boolean mappedOnly) {
        if (branchId == null) return getAllServices();
        if (mappedOnly) return getMappedServicesByBranch(branchId);

        Branch branch = requireBranch(branchId);
        if (serviceType != null && !serviceType.isBlank()) {
            return getServicesByBranchAndType(branch, serviceType);
        }
        return getServicesByBranch(branch);
    }

    public Services createService(ServiceRequest request) {
        Branch branch = requireBranch(request.branch().branchId());
        return createService(serviceDtoMapper.toEntity(request, branch));
    }

    public Services updateService(Long serviceId, ServiceRequest request) {
        Services updatedService = serviceDtoMapper.toEntity(request, requireBranch(request.branch().branchId()));
        return updateService(serviceId, updatedService);
    }

    @Cacheable(cacheNames = "services", key = "'branch:' + #branch.branchId")
    public List<Services> getServicesByBranch(Branch branch) {
        requireOperationalBranchAccess(branch.getBranchId());
        return serviceRepository.findByBranch(branch);
    }

    @Cacheable(cacheNames = "services", key = "'branch:' + #branch.branchId + ':type:' + #serviceType")
    public List<Services> getServicesByBranchAndType(Branch branch, String serviceType) {
        requireOperationalBranchAccess(branch.getBranchId());
        return serviceRepository.findByBranchAndServiceType(branch, serviceType);
    }

    @Cacheable(cacheNames = "services", key = "'mapped:' + #branchId")
    /** Chỉ trả dịch vụ đang được ít nhất một máy bốc số của chi nhánh cung cấp. */
    public List<Services> getMappedServicesByBranch(Long branchId) {
        requireOperationalBranchAccess(branchId);
        return mappingRepository.findActiveMappedServicesByBranchId(branchId);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "services", allEntries = true),
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    })
    @Transactional
    public Services createService(Services service) {
        validateFormSchema(service.getFormSchema());
        service.setRequiredCustomerFields(CustomerProfilePolicy.includeDefaults(service.getRequiredCustomerFields()));
        currentUserService.requireBranch(service.getBranch().getBranchId());
        service.setBranch(currentUserService.requireUser().getBranch());
        if (serviceRepository.existsByBranchAndServiceCode(
                service.getBranch(),
                service.getServiceCode())) {
            throw new RuntimeException("Mã dịch vụ đã tồn tại trong chi nhánh này");
        }
        if (serviceRepository.existsByBranchAndServiceNameIgnoreCase(
                service.getBranch(),
                service.getServiceName())) {
            throw new RuntimeException("Tên dịch vụ đã tồn tại trong chi nhánh này");
        }

        Services savedService = serviceRepository.save(service);
        eventPublisher.publish(
                "SERVICE_CREATED",
                "SERVICE",
                savedService.getServiceId().toString(),
                savedService.getBranch().getBranchId(),
                Map.of(
                        "serviceCode", savedService.getServiceCode(),
                        "serviceName", savedService.getServiceName()));

        return savedService;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "services", allEntries = true),
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    })
    @Transactional
    public Services updateService(Long serviceId, Services updatedService) {
        validateFormSchema(updatedService.getFormSchema());
        Services existingService = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));

        currentUserService.requireBranch(existingService.getBranch().getBranchId());
        currentUserService.requireBranch(updatedService.getBranch().getBranchId());

        if (serviceRepository.existsByBranchAndServiceCodeAndServiceIdNot(
                updatedService.getBranch(),
                updatedService.getServiceCode(),
                serviceId)) {
            throw new RuntimeException("Mã dịch vụ đã tồn tại trong chi nhánh này");
        }
        if (serviceRepository.existsByBranchAndServiceNameIgnoreCaseAndServiceIdNot(
                updatedService.getBranch(),
                updatedService.getServiceName(),
                serviceId)) {
            throw new RuntimeException("Tên dịch vụ đã tồn tại trong chi nhánh này");
        }

        existingService.setServiceCode(updatedService.getServiceCode());
        existingService.setServiceName(updatedService.getServiceName());
        existingService.setServiceType(updatedService.getServiceType());
        existingService.setDescription(updatedService.getDescription());
        existingService.setEstimatedTime(updatedService.getEstimatedTime());
        existingService.setStatus(updatedService.getStatus());
        existingService.setBranch(updatedService.getBranch());
        existingService.setRequiredCustomerFields(
                CustomerProfilePolicy.includeDefaults(updatedService.getRequiredCustomerFields()));
        existingService.setFormSchema(updatedService.getFormSchema());

        Services savedService = serviceRepository.save(existingService);
        eventPublisher.publish(
                "SERVICE_UPDATED",
                "SERVICE",
                savedService.getServiceId().toString(),
                savedService.getBranch().getBranchId(),
                Map.of(
                        "serviceCode", savedService.getServiceCode(),
                        "serviceName", savedService.getServiceName(),
                        "status", savedService.getStatus()));

        return savedService;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "services", allEntries = true),
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    })
    @Transactional
    public void deleteService(Long serviceId) {
        Services existingService = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));

        currentUserService.requireBranch(existingService.getBranch().getBranchId());

        List<Ticket> tickets = ticketRepository.findByService(existingService);
        boolean hasOpenTicket = tickets.stream()
                .anyMatch(ticket -> List.of("WAITING", "SERVING").contains(ticket.getStatus()));
        if (hasOpenTicket) {
            throw new RuntimeException("Không thể xóa dịch vụ vì còn phiếu đang chờ hoặc đang phục vụ");
        }

        List<Appointment> appointments = appointmentRepository.findByService(existingService);
        if (!appointments.isEmpty()) {
            throw new RuntimeException("Không thể xóa dịch vụ vì còn lịch hẹn đang gắn với dịch vụ này");
        }

        mappingRepository.deleteAll(mappingRepository.findByService(existingService));
        tickets.forEach(ticket -> ticket.setService(null));
        ticketRepository.saveAll(tickets);

        serviceRepository.delete(existingService);
        eventPublisher.publish(
                "SERVICE_DELETED",
                "SERVICE",
                serviceId.toString(),
                existingService.getBranch().getBranchId(),
                Map.of(
                        "serviceCode", existingService.getServiceCode(),
                        "serviceName", existingService.getServiceName()));
    }

    /** Bảo đảm BRANCH_ADMIN chỉ cấu hình dịch vụ thuộc chi nhánh mình phụ trách. */
    private void requireOperationalBranchAccess(Long branchId) {
        if (!"CUSTOMER".equals(currentUserService.requireUser().getRole())) {
            currentUserService.requireBranch(branchId);
        }
    }

    private Branch requireBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
    }

    private void validateFormSchema(List<FormFieldDefinition> fields) {
        if (fields == null || fields.size() > 50) {
            throw new RuntimeException("Bieu mau chi duoc phep toi da 50 truong");
        }
        Set<String> supportedTypes = Set.of("TEXT", "TEXTAREA", "NUMBER", "DATE", "SELECT", "RADIO", "CHECKBOX");
        Set<String> keys = new java.util.HashSet<>();
        for (FormFieldDefinition field : fields) {
            if (field.key() == null || !field.key().matches("^[A-Za-z][A-Za-z0-9_]{0,49}$") || !keys.add(field.key())) {
                throw new RuntimeException("Ma truong bieu mau khong hop le hoac bi trung");
            }
            if (field.label() == null || field.label().isBlank() || field.label().length() > 150
                    || field.section() == null || field.section().length() > 100
                    || !supportedTypes.contains(field.type())) {
                throw new RuntimeException("Cau hinh truong bieu mau khong hop le");
            }
            List<String> options = field.options() == null ? List.of() : field.options();
            if (options.size() > 30 || options.stream().anyMatch(option -> option == null || option.isBlank() || option.length() > 100)) {
                throw new RuntimeException("Danh sach lua chon khong hop le");
            }
            if (Set.of("SELECT", "RADIO").contains(field.type()) && options.isEmpty()) {
                throw new RuntimeException("Truong lua chon phai co it nhat mot gia tri");
            }
        }
    }
}
