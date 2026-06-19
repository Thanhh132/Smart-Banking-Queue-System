package com.sbqs.service;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.repository.QueueMachineServiceMappingRepository;
import com.sbqs.repository.ServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicesService {

    private final ServiceRepository serviceRepository;
    private final QueueMachineServiceMappingRepository mappingRepository;

    public ServicesService(
            ServiceRepository serviceRepository,
            QueueMachineServiceMappingRepository mappingRepository) {

        this.serviceRepository = serviceRepository;
        this.mappingRepository = mappingRepository;
    }

    public List<Services> getAllServices() {
        return serviceRepository.findAll();
    }

    public List<Services> getServicesByBranch(Branch branch) {
        return serviceRepository.findByBranch(branch);
    }

    public List<Services> getServicesByBranchAndType(Branch branch, String serviceType) {
        return serviceRepository.findByBranchAndServiceType(branch, serviceType);
    }

    public List<Services> getMappedServicesByBranch(Long branchId) {
        return mappingRepository.findByQueueMachineBranchBranchId(branchId)
                .stream()
                .map(mapping -> mapping.getService())
                .filter(service -> "ACTIVE".equalsIgnoreCase(service.getStatus()))
                .distinct()
                .toList();
    }

    public Services createService(Services service) {
        if (serviceRepository.existsByBranchAndServiceCode(
                service.getBranch(),
                service.getServiceCode())) {
            throw new RuntimeException("Ma dich vu da ton tai trong chi nhanh nay");
        }

        return serviceRepository.save(service);
    }

    public Services updateService(Long serviceId, Services updatedService) {
        Services existingService = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));

        if (serviceRepository.existsByBranchAndServiceCodeAndServiceIdNot(
                updatedService.getBranch(),
                updatedService.getServiceCode(),
                serviceId)) {
            throw new RuntimeException("Ma dich vu da ton tai trong chi nhanh nay");
        }

        existingService.setServiceCode(updatedService.getServiceCode());
        existingService.setServiceName(updatedService.getServiceName());
        existingService.setServiceType(updatedService.getServiceType());
        existingService.setDescription(updatedService.getDescription());
        existingService.setEstimatedTime(updatedService.getEstimatedTime());
        existingService.setStatus(updatedService.getStatus());
        existingService.setBranch(updatedService.getBranch());

        return serviceRepository.save(existingService);
    }

    public void deleteService(Long serviceId) {
        Services existingService = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay dich vu"));

        try {
            serviceRepository.delete(existingService);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Dich vu dang duoc gan voi may boc so. Vui long go mapping truoc khi xoa.");
        }
    }
}
