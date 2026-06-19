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

        public Services updateService(Long serviceId,
                        Services updatedService) {

                Services existingService = serviceRepository.findById(serviceId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Không tìm thấy dịch vụ"));

                existingService.setServiceCode(
                                updatedService.getServiceCode());

                existingService.setServiceName(
                                updatedService.getServiceName());

                existingService.setServiceType(
                                updatedService.getServiceType());

                existingService.setDescription(
                                updatedService.getDescription());

                existingService.setEstimatedTime(
                                updatedService.getEstimatedTime());

                existingService.setStatus(
                                updatedService.getStatus());

                existingService.setBranch(
                                updatedService.getBranch());

                return serviceRepository.save(existingService);
        }

        public void deleteService(Long serviceId) {

                Services existingService = serviceRepository.findById(serviceId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));

                try {

                        serviceRepository.delete(existingService);

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "Dịch vụ đang được gán với máy bốc số. Vui lòng gỡ mapping trước khi xóa.");
                }
        }
}