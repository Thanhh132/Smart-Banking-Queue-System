package com.sbqs.controller;

import com.sbqs.dto.QueueMonitorResponse;
import com.sbqs.service.QueueMonitorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queue-monitor")
@CrossOrigin("*")
public class QueueMonitorController {

    private final QueueMonitorService queueMonitorService;

    public QueueMonitorController(
            QueueMonitorService queueMonitorService) {

        this.queueMonitorService = queueMonitorService;
    }

    @GetMapping
    public QueueMonitorResponse getMonitor(
            @RequestParam Long branchId) {

        return queueMonitorService.getMonitor(branchId);
    }
}