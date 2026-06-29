package com.sbqs.controller;

import com.sbqs.dto.bulkimport.ImportResult;
import com.sbqs.service.BulkImportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/import")
public class BulkImportController {
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final BulkImportService bulkImportService;

    public BulkImportController(BulkImportService bulkImportService) {
        this.bulkImportService = bulkImportService;
    }

    @GetMapping("/templates/staff")
    public ResponseEntity<byte[]> staffTemplate() {
        return template(bulkImportService.staffTemplate(), "sbqs-staff-import-template.xlsx");
    }

    @GetMapping("/templates/services")
    public ResponseEntity<byte[]> serviceTemplate() {
        return template(bulkImportService.serviceTemplate(), "sbqs-services-import-template.xlsx");
    }

    @PostMapping(value = "/staff", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importStaff(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bulkImportService.importStaff(file));
    }

    @PostMapping(value = "/services", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importServices(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(bulkImportService.importServices(file));
    }

    private ResponseEntity<byte[]> template(byte[] content, String fileName) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, XLSX_CONTENT_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(content);
    }
}
