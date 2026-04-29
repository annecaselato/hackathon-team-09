package com.datacorp.sifap.payment;

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
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment management — BATCHPGT/CALCCORR equivalent")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create payment (REQ-PAY-001 to 003)")
    public ResponseEntity<PaymentResponse> create(
        @Valid @RequestBody PaymentRequest request,
        @AuthenticationPrincipal UserDetails user) {

        Payment saved = service.create(request, user != null ? user.getUsername() : "SYSTEM");
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(PaymentResponse.from(saved));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(PaymentResponse.from(service.findById(id)));
    }

    @GetMapping
    @Operation(summary = "List payments with filters (REQ-API-003, REQ-API-004)")
    public ResponseEntity<Page<PaymentResponse>> list(
        @RequestParam(required = false) String cpf,
        @RequestParam(required = false) String anoMesRef,
        @RequestParam(required = false) PaymentStatus status,
        @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(service.list(cpf, anoMesRef, status, pageable).map(PaymentResponse::from));
    }

    @PatchMapping("/{id}/correction")
    @Operation(summary = "Apply payment correction (REQ-PAY-004)")
    public ResponseEntity<PaymentResponse> correct(
        @PathVariable Long id,
        @RequestBody PaymentCorrectionRequest request,
        @AuthenticationPrincipal UserDetails user) {

        Payment updated = service.correct(id, request, user != null ? user.getUsername() : "SYSTEM");
        return ResponseEntity.ok(PaymentResponse.from(updated));
    }
}
