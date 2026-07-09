package com.sbqs.service;

import com.sbqs.event.DomainEventPublisher;
import com.sbqs.entity.Appointment;
import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.entity.Ticket;
import com.sbqs.repository.AppointmentRepository;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceRepository;
import com.sbqs.repository.TicketRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ServicesService {

    private final ServiceRepository serviceRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;
    private final TicketRepository ticketRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentUserService currentUserService;
    private final DomainEventPublisher eventPublisher;

    public ServicesService(
            ServiceRepository serviceRepository,
            QueueMachineServiceMappingRepository mappingRepository,
            TicketRepository ticketRepository,
            AppointmentRepository appointmentRepository,
            CurrentUserService currentUserService,
            DomainEventPublisher eventPublisher) {

        this.serviceRepository = serviceRepository;
        this.mappingRepository = mappingRepository;
        this.ticketRepository = ticketRepository;
        this.appointmentRepository = appointmentRepository;
        this.currentUserService = currentUserService;
        this.eventPublisher = eventPublisher;
    }

    public List<Services> getAllServices() {
        return serviceRepository.findByBranch(currentUserService.requireUser().getBranch());
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
        return mappingRepository.findByQueueMachineBranchBranchId(branchId)
                .stream()
                .map(mapping -> mapping.getService())
                .filter(service -> "ACTIVE".equalsIgnoreCase(service.getStatus()))
                .distinct()
                .toList();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "services", allEntries = true),
            @CacheEvict(cacheNames = "queueMonitor", allEntries = true)
    })
    public Services createService(Services service) {
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
    public Services updateService(Long serviceId, Services updatedService) {
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
        existingService.setRequiredCustomerFields(updatedService.getRequiredCustomerFields());

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
}
