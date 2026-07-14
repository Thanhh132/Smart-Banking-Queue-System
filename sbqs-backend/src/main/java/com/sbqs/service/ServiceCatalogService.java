package com.sbqs.service;

import com.sbqs.dto.service.ServiceCatalogRequest;
import com.sbqs.entity.ServiceCatalog;
import com.sbqs.entity.Services;
import com.sbqs.repository.ServiceCatalogRepository;
import com.sbqs.repository.ServiceRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceCatalogService {
    private final ServiceCatalogRepository catalogRepository;
    private final ServiceRepository serviceRepository;
    private final CurrentUserService currentUserService;

    public ServiceCatalogService(ServiceCatalogRepository catalogRepository,
                                 ServiceRepository serviceRepository,
                                 CurrentUserService currentUserService) {
        this.catalogRepository = catalogRepository;
        this.serviceRepository = serviceRepository;
        this.currentUserService = currentUserService;
    }

    public List<ServiceCatalog> getCatalog() {
        return catalogRepository.findAllByOrderByServiceNameAsc();
    }

    @Transactional
    public ServiceCatalog create(ServiceCatalogRequest request) {
        String code = request.serviceCode().trim().toUpperCase();
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
        item.setStatus("ACTIVE");
        item.setFormSchema(new ArrayList<>());
        return catalogRepository.save(item);
    }

    @Transactional
    @CacheEvict(cacheNames = {"services", "queueMonitor"}, allEntries = true)
    public Services addToCurrentBranch(Long catalogId) {
        var user = currentUserService.requireUser();
        if (user.getBranch() == null) {
            throw new RuntimeException("Tài khoản chưa được gán chi nhánh");
        }
        ServiceCatalog item = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ trong danh mục hệ thống"));
        if (!"ACTIVE".equals(item.getStatus())) {
            throw new RuntimeException("Dịch vụ này hiện không còn được cung cấp trong danh mục hệ thống");
        }
        if (serviceRepository.existsByBranchAndServiceCode(user.getBranch(), item.getServiceCode())) {
            throw new RuntimeException("Chi nhánh đã thêm dịch vụ này");
        }

        Services service = new Services();
        service.setCatalog(item);
        service.setBranch(user.getBranch());
        service.setServiceCode(item.getServiceCode());
        service.setServiceName(item.getServiceName());
        service.setServiceType(item.getServiceType());
        service.setDescription(item.getDescription());
        service.setEstimatedTime(item.getEstimatedTime());
        service.setStatus("ACTIVE");
        service.setRequiredCustomerFields(CustomerProfilePolicy.includeDefaults(List.of()));
        service.setFormSchema(new ArrayList<>(item.getFormSchema()));
        return serviceRepository.save(service);
    }
}
