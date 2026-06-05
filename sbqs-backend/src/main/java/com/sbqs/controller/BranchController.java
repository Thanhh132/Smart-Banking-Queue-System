package com.sbqs.controller;

import com.sbqs.entity.Branch;
import com.sbqs.service.BranchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public ResponseEntity<List<Branch>> getAllBranches(@RequestParam(required = false) String bankName) {
        List<Branch> branches;
        if (bankName != null && !bankName.isEmpty()) {
            branches = branchService.getBranchesByBank(bankName);
        } else {
            branches = branchService.getAllBranches();
        }
        return ResponseEntity.ok(branches);
    }

    @GetMapping("/nearest")
    public ResponseEntity<List<Branch>> getNearestBranches(
            @RequestParam String bankName,
            @RequestParam double latitude,
            @RequestParam double longitude) {
        List<Branch> branches = branchService.getNearestBranches(bankName, latitude, longitude);
        return ResponseEntity.ok(branches);
    }

    @PostMapping
    public ResponseEntity<Branch> createBranch(@RequestBody Branch branch) {
        Branch saved = branchService.createBranch(branch);
        return ResponseEntity.ok(saved);
    }
}