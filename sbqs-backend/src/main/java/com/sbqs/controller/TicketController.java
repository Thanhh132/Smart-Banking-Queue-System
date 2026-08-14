package com.sbqs.controller;

import com.sbqs.entity.Ticket;
import com.sbqs.dto.TicketStaffViewResponse;
import com.sbqs.dto.TicketTrackingResponse;
import com.sbqs.dto.CreatePreparedTicketRequest;
import jakarta.validation.Valid;
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

    /** Chỉ chủ sở hữu phiếu được xem trạng thái realtime và số khách đang chờ trước mình. */
    @GetMapping("/{ticketId}/tracking")
    public ResponseEntity<TicketTrackingResponse> trackCustomerTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.trackCustomerTicket(ticketId));
    }

    @PostMapping
    public ResponseEntity<Ticket> createTicket(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody Ticket ticket) {

        return ResponseEntity.ok(
                ticketService.createTicket(ticket, idempotencyKey));
    }

    /** Gọi phiếu WAITING đầu tiên phù hợp với máy bốc số của quầy nhân viên đang giữ. */
    @PostMapping("/call-next")
    public ResponseEntity<TicketStaffViewResponse> callNextTicket(
            @RequestParam Long counterId) {

        return ResponseEntity.ok(ticketService.callNextTicket(counterId));
    }

    /** Xác thực biểu mẫu khai trước, cấp số và lưu snapshot dữ liệu giao dịch trong một luồng. */
    @PostMapping("/prepared")
    public ResponseEntity<Ticket> createPreparedTicket(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePreparedTicketRequest request) {
        return ResponseEntity.ok(ticketService.createPreparedTicket(request, idempotencyKey));
    }

    @GetMapping("/{ticketId}/staff-view")
    public ResponseEntity<TicketStaffViewResponse> getStaffTicketView(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getServingTicketForStaff(ticketId));
    }

    /** Hoàn tất phiếu, giải phóng quầy và tạo bản ghi lịch sử bất biến phục vụ báo cáo. */
    @PostMapping("/{ticketId}/complete")
    public ResponseEntity<Ticket> completeTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.completeTicket(ticketId));
    }

    @PostMapping("/{ticketId}/no-show")
    public ResponseEntity<Ticket> markCustomerNoShow(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.markCustomerNoShow(ticketId));
    }

    @PostMapping("/{ticketId}/cancel")
    public ResponseEntity<Ticket> cancelTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.cancelTicket(ticketId));
    }
}
