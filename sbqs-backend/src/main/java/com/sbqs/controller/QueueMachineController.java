package com.sbqs.controller;

import com.sbqs.entity.QueueMachine;
import com.sbqs.service.QueueMachineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue-machines")
@CrossOrigin("*")
public class QueueMachineController {

    private final QueueMachineService queueMachineService;

    public QueueMachineController(QueueMachineService queueMachineService) {
        this.queueMachineService = queueMachineService;
    }

    @GetMapping
    public ResponseEntity<List<QueueMachine>> getAllQueueMachines() {
        return ResponseEntity.ok(queueMachineService.getAllQueueMachines());
    }

    @PostMapping
    public ResponseEntity<QueueMachine> createQueueMachine(
            @RequestBody QueueMachine queueMachine) {

        return ResponseEntity.ok(
                queueMachineService.createQueueMachine(queueMachine));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QueueMachine> updateQueueMachine(
            @PathVariable Long id,
            @RequestBody QueueMachine queueMachine) {

        return ResponseEntity.ok(
                queueMachineService.updateQueueMachine(id, queueMachine));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteQueueMachine(
            @PathVariable Long id) {

        queueMachineService.deleteQueueMachine(id);

        return ResponseEntity.ok("Khóa máy bốc số thành công");
    }
}