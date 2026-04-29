package com.datacorp.sifap.audit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Audit trail — RELAUDIT equivalent (REQ-AUD-001, REQ-AUD-002)")
public class AuditController {

    private final AuditRepository repository;

    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Standard audit report (excludes EX actions — REQ-AUD-002)")
    public ResponseEntity<Page<AuditRecord>> standardReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @PageableDefault(size = 50) Pageable pageable) {

        return ResponseEntity.ok(repository.findStandardReport(from, to, pageable));
    }

    @GetMapping("/full")
    @Operation(summary = "Full audit report including EX actions (requires explicit request)")
    public ResponseEntity<Page<AuditRecord>> fullReport(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @PageableDefault(size = 50) Pageable pageable) {

        return ResponseEntity.ok(repository.findFullReport(from, to, pageable));
    }

    @GetMapping("/entity/{type}/{id}")
    @Operation(summary = "Get audit trail for specific entity")
    public ResponseEntity<Page<AuditRecord>> byEntity(
        @PathVariable String type,
        @PathVariable String id,
        @PageableDefault(size = 50) Pageable pageable) {

        return ResponseEntity.ok(
            repository.findByTipoEntidadeAndIdEntidade(type.toUpperCase(), id, pageable));
    }
}
