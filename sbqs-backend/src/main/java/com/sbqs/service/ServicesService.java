package com.sbqs.service;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.repository.ServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicesService {

    private final ServiceRepository serviceRepository;

    public ServicesService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
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

    public Services createService(Services service) {
        return serviceRepository.save(service);
    }
}