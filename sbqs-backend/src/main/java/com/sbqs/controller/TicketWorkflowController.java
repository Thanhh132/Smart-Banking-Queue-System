package com.sbqs.controller;

import com.sbqs.dto.TicketWorkflowTaskResponse;
import com.sbqs.service.TicketWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflows/tickets")
public class TicketWorkflowController {

    private final TicketWorkflowService ticketWorkflowService;

    public TicketWorkflowController(TicketWorkflowService ticketWorkflowService) {
        this.ticketWorkflowService = ticketWorkflowService;
    }

    @GetMapping("/pending-approval")
    public ResponseEntity<List<TicketWorkflowTaskResponse>> getPendingApprovalTasks() {
        return ResponseEntity.ok(ticketWorkflowService.getPendingApprovalTasks());
    }
}
