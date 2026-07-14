package com.sbqs.controller;

import com.sbqs.dto.BranchHoursRequest;
import com.sbqs.dto.BranchHoursResponse;
import com.sbqs.dto.BranchOpenStatusResponse;
import com.sbqs.service.BranchOperatingHoursService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/branch-hours")
public class BranchOperatingHoursController {
    private final BranchOperatingHoursService service;
    public BranchOperatingHoursController(BranchOperatingHoursService service) { this.service = service; }

    @GetMapping("/{branchId}") public List<BranchHoursResponse> get(@PathVariable Long branchId) { return service.getSchedule(branchId); }
    @GetMapping("/{branchId}/status") public BranchOpenStatusResponse status(@PathVariable Long branchId) { return service.getStatus(branchId); }
    @PutMapping("/current") public List<BranchHoursResponse> update(@Valid @RequestBody List<@Valid BranchHoursRequest> requests) {
        return service.updateCurrentBranch(requests);
    }
}
