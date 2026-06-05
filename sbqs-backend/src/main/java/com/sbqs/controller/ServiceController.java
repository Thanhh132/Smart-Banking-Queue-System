package com.sbqs.controller;

import com.sbqs.entity.Branch;
import com.sbqs.entity.Services;
import com.sbqs.repository.BranchRepository;
import com.sbqs.service.ServicesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServicesService serviceService;
    private final BranchRepository branchRepository;

    public ServiceController(ServicesService serviceService, BranchRepository branchRepository) {
        this.serviceService = serviceService;
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public ResponseEntity<List<Services>> getServices(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String serviceType
    ) {
        if (branchId == null) {
            return ResponseEntity.ok(serviceService.getAllServices());
        }

        Branch branch = branchRepository.findById(branchId).orElse(null);
        if (branch == null) {
            return ResponseEntity.notFound().build();
        }

        if (serviceType != null && !serviceType.isBlank()) {
            return ResponseEntity.ok(serviceService.getServicesByBranchAndType(branch, serviceType));
        }

        return ResponseEntity.ok(serviceService.getServicesByBranch(branch));
    }

    @PostMapping
    public ResponseEntity<Services> createService(@RequestBody Services service) {
        Services saved = serviceService.createService(service);
        return ResponseEntity.ok(saved);
    }
}