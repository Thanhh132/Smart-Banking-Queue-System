package com.sbqs.controller;

import com.sbqs.entity.Ticket;
import com.sbqs.dto.TicketStaffViewResponse;
import com.sbqs.dto.TicketTrackingResponse;
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

    @GetMapping("/current")
    public ResponseEntity<Ticket> getCurrentCustomerTicket() {
        return ResponseEntity.ok(ticketService.getCurrentCustomerTicket());
    }

    @GetMapping("/{ticketId}/tracking")
    public ResponseEntity<TicketTrackingResponse> trackCustomerTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.trackCustomerTicket(ticketId));
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(
            @RequestBody Ticket ticket) {

        return ResponseEntity.ok(
                ticketService.createTicket(ticket));
    }

    @PostMapping("/call-next")
    public ResponseEntity<TicketStaffViewResponse> callNextTicket(
            @RequestParam Long counterId) {

        return ResponseEntity.ok(ticketService.callNextTicket(counterId));
    }

    @GetMapping("/{ticketId}/staff-view")
    public ResponseEntity<TicketStaffViewResponse> getStaffTicketView(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getServingTicketForStaff(ticketId));
    }

    @PostMapping("/{ticketId}/complete")
    public ResponseEntity<Ticket> completeTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.completeTicket(ticketId));
    }

    @PostMapping("/{ticketId}/cancel")
    public ResponseEntity<Ticket> cancelTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.cancelTicket(ticketId));
    }
}
