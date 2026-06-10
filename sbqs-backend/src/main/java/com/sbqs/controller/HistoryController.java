package com.sbqs.controller;

import com.sbqs.dto.HistoryResponse;
import com.sbqs.service.HistoryService;
import org.springframework.web.bind.annotation.*;

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
    public List<HistoryResponse> getAllHistory() {
        return historyService.getAllHistory();
    }
}