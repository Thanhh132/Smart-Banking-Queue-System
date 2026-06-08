package com.sbqs.controller;

import com.sbqs.service.QueueStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/queue")
public class QueueStatusController {

    private final QueueStatusService queueStatusService;

    public QueueStatusController(QueueStatusService queueStatusService) {
        this.queueStatusService = queueStatusService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        return ResponseEntity.ok(queueStatusService.getQueueStatus());
    }
}