package com.sbqs.controller;

import com.sbqs.dto.report.ReportDocument;
import com.sbqs.dto.report.ReportFormat;
import com.sbqs.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/users")
    public ResponseEntity<byte[]> users(@RequestParam(defaultValue = "pdf") String format) {
        return response(reportService.exportUsers(ReportFormat.from(format)));
    }

    @GetMapping("/services")
    public ResponseEntity<byte[]> services(@RequestParam(defaultValue = "pdf") String format) {
        return response(reportService.exportServices(ReportFormat.from(format)));
    }

    @GetMapping("/tickets")
    public ResponseEntity<byte[]> tickets(@RequestParam(defaultValue = "pdf") String format) {
        return response(reportService.exportTickets(ReportFormat.from(format)));
    }

    @GetMapping("/history")
    public ResponseEntity<byte[]> history(@RequestParam(defaultValue = "pdf") String format) {
        return response(reportService.exportHistory(ReportFormat.from(format)));
    }

    private ResponseEntity<byte[]> response(ReportDocument document) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, document.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(document.content());
    }
}
