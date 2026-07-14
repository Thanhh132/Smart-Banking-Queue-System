package com.sbqs.controller;

import com.sbqs.entity.Branch;
import com.sbqs.service.BranchService;
import com.sbqs.service.SmartRoutingService;
import com.sbqs.dto.SmartRoutingRecommendationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;
    private final SmartRoutingService smartRoutingService;

    public BranchController(BranchService branchService, SmartRoutingService smartRoutingService) {
        this.branchService = branchService;
        this.smartRoutingService = smartRoutingService;
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

    /** Trả các chi nhánh cùng ngân hàng theo khoảng cách tăng dần từ tọa độ khách hàng. */
    @GetMapping("/nearest")
    public ResponseEntity<List<Branch>> getNearestBranches(
            @RequestParam String bankName,
            @RequestParam double latitude,
            @RequestParam double longitude) {
        List<Branch> branches = branchService.getNearestBranches(bankName, latitude, longitude);
        return ResponseEntity.ok(branches);
    }

    /** Xếp hạng chi nhánh theo 40% khoảng cách và 60% thời gian chờ ước tính mặc định. */
    @GetMapping("/recommendations")
    public ResponseEntity<List<SmartRoutingRecommendationResponse>> getRecommendations(
            @RequestParam(required = false) String bankName,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) String serviceCode) {
        return ResponseEntity.ok(smartRoutingService.recommend(bankName, latitude, longitude, serviceCode));
    }

    /** Tạo chi nhánh; service sẽ sinh lại mã nếu mã gửi lên bị thiếu hoặc đã tồn tại. */
    @PostMapping
    public ResponseEntity<Branch> createBranch(@RequestBody Branch branch) {
        Branch saved = branchService.createBranch(branch);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Branch> updateBranch(
            @PathVariable Long id,
            @RequestBody Branch branch) {

        return ResponseEntity.ok(branchService.updateBranch(id, branch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }
}
