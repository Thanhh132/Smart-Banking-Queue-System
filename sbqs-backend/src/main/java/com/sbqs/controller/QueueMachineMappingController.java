package com.sbqs.controller;

import com.sbqs.dto.MappingRequest;
import com.sbqs.entity.QueueMachineServiceMapping;
import com.sbqs.service.QueueMachineMappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue-machine-mappings")
@CrossOrigin("*")
public class QueueMachineMappingController {

    private final QueueMachineMappingService mappingService;

    public QueueMachineMappingController(
            QueueMachineMappingService mappingService) {

        this.mappingService = mappingService;
    }

    @GetMapping
    public ResponseEntity<List<QueueMachineServiceMapping>> getAllMappings() {
        return ResponseEntity.ok(mappingService.getAllMappings());
    }

    @PostMapping
    public ResponseEntity<QueueMachineServiceMapping> createMapping(
            @RequestBody MappingRequest request) {

        return ResponseEntity.ok(mappingService.createMapping(request));
    }

    @DeleteMapping
    public ResponseEntity<String> deleteMapping(
            @RequestBody MappingRequest request) {

        mappingService.deleteMapping(request);

        return ResponseEntity.ok("Xóa mapping thành công");
    }
}