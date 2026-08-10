package com.sbqs.controller;

import com.sbqs.dto.service.ServiceCatalogRequest;
import com.sbqs.dto.service.ServiceCatalogResponse;
import com.sbqs.entity.ServiceCatalog;
import com.sbqs.service.ServiceCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/service-catalog")
public class ServiceCatalogController {
    private final ServiceCatalogService catalogService;

    public ServiceCatalogController(ServiceCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ServiceCatalogResponse> getCatalog() {
        return catalogService.getCatalog().stream().map(this::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<ServiceCatalogResponse> create(@Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.ok(toResponse(catalogService.create(request)));
    }

    @PutMapping("/{catalogId}")
    public ResponseEntity<ServiceCatalogResponse> update(@PathVariable Long catalogId,
                                                         @Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.ok(toResponse(catalogService.update(catalogId, request)));
    }

    @DeleteMapping("/{catalogId}")
    public ResponseEntity<Void> delete(@PathVariable Long catalogId) {
        catalogService.delete(catalogId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{catalogId}/restore")
    public ResponseEntity<ServiceCatalogResponse> restore(@PathVariable Long catalogId) {
        return ResponseEntity.ok(toResponse(catalogService.restore(catalogId)));
    }

    private ServiceCatalogResponse toResponse(ServiceCatalog item) {
        return new ServiceCatalogResponse(item.getCatalogId(), item.getServiceCode(), item.getServiceName(),
                item.getServiceType(), item.getDescription(), item.getEstimatedTime(), item.getStatus(),
                item.isDelegatable());
    }
}
