package com.sbqs.controller;

import com.sbqs.entity.Counter;
import com.sbqs.entity.Branch;
import com.sbqs.repository.BranchRepository;
import com.sbqs.service.CounterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/counters")
public class CounterController {
    private final CounterService counterService;
    private final BranchRepository branchRepository;

    public CounterController(CounterService counterService, BranchRepository branchRepository) {
        this.counterService = counterService;
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public ResponseEntity<List<Counter>> getCounters(@RequestParam(required = false) Long branchId) {
        List<Counter> counters;
        if (branchId != null) {
            Branch branch = branchRepository.findById(branchId).orElse(null);
            if (branch == null) return ResponseEntity.notFound().build();
            counters = counterService.getCountersByBranch(branch);
        } else {
            counters = counterService.getAllCounters();
        }
        return ResponseEntity.ok(counters);
    }

    @PostMapping
    public ResponseEntity<Counter> createCounter(@RequestBody Counter counter) {
        Counter saved = counterService.createCounter(counter);
        return ResponseEntity.ok(saved);
    }
}