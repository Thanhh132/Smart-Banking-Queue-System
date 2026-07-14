package com.sbqs.controller;

import com.sbqs.dto.delegation.*;
import com.sbqs.service.DigitalDelegationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/delegations")
public class DigitalDelegationController {
    private final DigitalDelegationService service;
    public DigitalDelegationController(DigitalDelegationService service) { this.service = service; }
    @GetMapping("/mine") public List<DelegationResponse> mine() { return service.getMine(); }
    @PostMapping public DelegationResponse create(@Valid @RequestBody CreateDelegationRequest request) { return service.create(request); }
    @PostMapping("/{id}/cancel") public DelegationResponse cancel(@PathVariable Long id) { return service.cancel(id); }
    @PostMapping("/verify") public DelegationResponse verify(@Valid @RequestBody VerifyDelegationRequest request) { return service.verify(request); }
    @PostMapping("/{id}/use") public DelegationResponse use(@PathVariable Long id) { return service.markUsed(id); }
}
