package com.sbqs.controller;

import com.sbqs.dto.HistoryResponse;
import com.sbqs.service.HistoryService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@CrossOrigin("*")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<HistoryResponse> getAllHistory(

            @RequestParam(required = false) Long branchId,

            @RequestParam(required = false) LocalDate from,

            @RequestParam(required = false) LocalDate to) {

        if (from != null && to != null) {
            return historyService
                    .getHistoryByDateRange(from, to);
        }

        if (branchId != null) {
            return historyService
                    .getHistoryByBranch(branchId);
        }

        return historyService.getAllHistory();
    }
}