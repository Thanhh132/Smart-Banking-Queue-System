package com.sbqs.controller;

import com.sbqs.dto.service.ServiceCatalogRequest;
import com.sbqs.dto.service.ServiceCatalogResponse;
import com.sbqs.dto.service.ServiceResponse;
import com.sbqs.entity.ServiceCatalog;
import com.sbqs.mapper.ServiceDtoMapper;
import com.sbqs.service.ServiceCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/service-catalog")
public class ServiceCatalogController {
    private final ServiceCatalogService catalogService;
    private final ServiceDtoMapper serviceDtoMapper;

    public ServiceCatalogController(ServiceCatalogService catalogService, ServiceDtoMapper serviceDtoMapper) {
        this.catalogService = catalogService;
        this.serviceDtoMapper = serviceDtoMapper;
    }

    @GetMapping
    public List<ServiceCatalogResponse> getCatalog() {
        return catalogService.getCatalog().stream().map(this::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<ServiceCatalogResponse> create(@Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.ok(toResponse(catalogService.create(request)));
    }

    @PostMapping("/{catalogId}/add-to-branch")
    public ResponseEntity<ServiceResponse> addToBranch(@PathVariable Long catalogId) {
        return ResponseEntity.ok(serviceDtoMapper.toResponse(catalogService.addToCurrentBranch(catalogId)));
    }

    private ServiceCatalogResponse toResponse(ServiceCatalog item) {
        return new ServiceCatalogResponse(item.getCatalogId(), item.getServiceCode(), item.getServiceName(),
                item.getServiceType(), item.getDescription(), item.getEstimatedTime(), item.getStatus());
    }
}
