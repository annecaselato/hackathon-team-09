package com.datacorp.sifap.beneficiary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@Tag(name = "Beneficiaries", description = "Beneficiary management — CADBENEF/CONSBENF equivalent")
public class BeneficiaryController {

    private final BeneficiaryService service;

    public BeneficiaryController(BeneficiaryService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register new beneficiary (REQ-BEN-001 to 005)")
    public ResponseEntity<BeneficiaryResponse> register(
        @Valid @RequestBody BeneficiaryRequest request,
        @AuthenticationPrincipal UserDetails user) {

        Beneficiary saved = service.register(request, user != null ? user.getUsername() : "SYSTEM");
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(BeneficiaryResponse.from(saved));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get beneficiary by ID")
    public ResponseEntity<BeneficiaryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(BeneficiaryResponse.from(service.findById(id)));
    }

    @GetMapping("/cpf/{cpf}")
    @Operation(summary = "Get beneficiary by CPF")
    public ResponseEntity<BeneficiaryResponse> getByCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(BeneficiaryResponse.from(service.findByCpf(cpf)));
    }

    @GetMapping
    @Operation(summary = "List beneficiaries with optional filters (REQ-API-003, REQ-API-004)")
    public ResponseEntity<Page<BeneficiaryResponse>> list(
        @RequestParam(required = false) BeneficiaryStatus status,
        @RequestParam(required = false) String nome,
        @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(service.list(status, nome, pageable).map(BeneficiaryResponse::from));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update beneficiary status")
    public ResponseEntity<BeneficiaryResponse> updateStatus(
        @PathVariable Long id,
        @RequestParam BeneficiaryStatus status,
        @AuthenticationPrincipal UserDetails user) {

        Beneficiary updated = service.updateStatus(id, status, user != null ? user.getUsername() : "SYSTEM");
        return ResponseEntity.ok(BeneficiaryResponse.from(updated));
    }
}
