package com.sbqs.controller;

import com.sbqs.dto.service.ServiceRequest;
import com.sbqs.dto.service.ServiceResponse;
import com.sbqs.mapper.ServiceDtoMapper;
import com.sbqs.service.ServicesService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServicesService serviceService;
    private final ServiceDtoMapper serviceDtoMapper;

    public ServiceController(ServicesService serviceService, ServiceDtoMapper serviceDtoMapper) {
        this.serviceService = serviceService;
        this.serviceDtoMapper = serviceDtoMapper;
    }

    /**
     * Lọc danh mục theo chi nhánh/loại dịch vụ; mappedOnly chỉ trả các dịch vụ đã
     * được gắn vào máy bốc số và vì vậy khách hàng có thể lấy số thực tế.
     */
    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getServices(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String serviceType,
            @RequestParam(defaultValue = "false") boolean mappedOnly) {
        return ResponseEntity.ok(serviceService.getServices(branchId, serviceType, mappedOnly)
                .stream().map(serviceDtoMapper::toResponse).toList());
    }

    /** Tạo dịch vụ kèm schema biểu mẫu giao dịch sau khi DTO và nghiệp vụ được kiểm tra. */
    @PostMapping
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serviceDtoMapper.toResponse(serviceService.createService(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serviceDtoMapper.toResponse(serviceService.updateService(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id) {

        serviceService.deleteService(id);

        return ResponseEntity.noContent().build();
    }
}
