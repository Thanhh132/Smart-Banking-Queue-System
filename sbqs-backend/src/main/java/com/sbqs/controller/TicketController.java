package com.sbqs.controller;

import com.sbqs.entity.Ticket;
import com.sbqs.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Ticket>> getTicketsByStatus(
            @PathVariable String status) {

        return ResponseEntity.ok(
                ticketService.getTicketsByStatus(status));
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(
            @RequestBody Ticket ticket) {

        return ResponseEntity.ok(
                ticketService.createTicket(ticket));
    }

    @PostMapping("/call-next")
    public ResponseEntity<Ticket> callNextTicket() {
        return ResponseEntity.ok(ticketService.callNextTicket());
    }
}