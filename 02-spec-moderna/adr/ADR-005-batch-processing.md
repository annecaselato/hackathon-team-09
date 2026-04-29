---
title: "ADR-005: Spring Batch for Payment Processing (Keep Batch, Add Real-Time APIs)"
status: "Accepted"
date: "2026-04-29"
deciders: ["Danilo Lisboa (Backend Dev)", "Vitor Filincowsky (Tech Lead)", "Anne Caselato (DevOps)"]
tags: ["stage-2", "adr", "batch", "real-time", "spring-batch", "payment"]
---

# ADR-005: Spring Batch for Payment Processing (Keep Batch, Add Real-Time APIs)

**Status**: Accepted  
**Date**: 2026-04-29  
**Deciders**: Danilo Lisboa (Backend Dev), Vitor Filincowsky (Tech Lead), Anne Caselato (DevOps)

---

## 📖 Context

SIFAP legacy system processes payments via **batch jobs**:
- `BATCHPGT.NSN`: Monthly payment generation (1st business day, 22:00h, 1.5-3.3h duration)
- `BATCHCON.NSN`: Bank reconciliation (monthly, after payment dispatch)
- `BATCHREL.NSN`: Payment release to bank (monthly)

The question is: should modernization **replace batch with real-time event-driven processing** or **keep batch paradigm with modern tooling**?

Additionally, operators need **real-time query capability** for individual beneficiary lookups — which the current 3270 terminal supports via `CONSBENF` but does not surface real-time batch status.

**Key constraint**: 4.2M beneficiary payments must be generated in a single monthly cycle. Bank deadline for payment files is typically 3-5 business days after generation. The batch nature is a business requirement driven by banking regulations (CNAB 240 standard).

---

## ⚖️ Decision

We will **keep batch processing for payment generation and reconciliation** (BATCHPGT, BATCHCON, BATCHREL) using **Spring Batch 5.x with partitioned processing**, while simultaneously **adding real-time REST APIs** for beneficiary queries, individual payment lookups, and audit reports.

**Hybrid Architecture**:
```
┌─────────────────────────────────────┐
│              SIFAP 2.0              │
│                                     │
│  Real-Time Path (REST APIs)         │
│  ├─ GET /beneficiaries (CONSBENF)   │
│  ├─ GET /payments/:id               │
│  ├─ GET /audit (RELAUDIT)           │
│  └─ POST /beneficiaries (CADBENEF)  │
│                                     │
│  Batch Path (Spring Batch)          │
│  ├─ PaymentGenerationJob (BATCHPGT) │
│  ├─ BankReconciliationJob (BATCHCON)│
│  └─ PaymentReleaseJob (BATCHREL)    │
│                                     │
│  Shared: PostgreSQL + JPA           │
└─────────────────────────────────────┘
```

**Spring Batch Configuration for BATCHPGT**:
- **Chunk size**: 50 beneficiaries per chunk (INSERT batch)
- **Partitions**: 20 parallel partitions (210,000 beneficiaries each)
- **Target duration**: 4.2M ÷ 20 partitions ÷ 50 chunk = 4,200 chunks; at 100ms/chunk = 420 seconds ≈ **7 minutes** (well under 1h SLA)

---

## 💡 Rationale

### Why Keep Batch

- **CNAB 240 requirement**: Brazilian banking standard mandates batch payment files (not real-time API calls to bank)
- **Volume**: 4.2M payments in a single cycle is inherently batch; real-time would require 4.2M API calls per month to the bank
- **Atomicity**: If batch fails at 50%, all generated records must be rolled back; Spring Batch provides restart-from-checkpoint capability
- **Business requirement**: Operators and compliance teams expect monthly payment cycles, not continuous processing; changing this is out of scope
- **Audit trail**: Batch audit records (COD-ACAO=BT) must capture cycle context (NUM-CICLO-BATCH) — only possible in batch mode

### Why ADD Real-Time (Hybrid)

- **Stage 1 Finding**: Legacy 3270 interface blocks operators from real-time lookups during batch; web UI can serve both simultaneously
- **Decoupled read/write**: Read operations (beneficiary search, payment history, audit reports) have no reason to be batch-only
- **REQ-PERF-002**: API response <500ms (p99) — impossible in batch mode; requires dedicated read path
- **User Experience**: Operators currently wait for batch completion to check status; real-time API solves this

### Spring Batch 5.x Advantages

- **Restartable**: Failed batch jobs restart from last checkpoint (chunk), not from beginning — critical for 4.2M record jobs
- **Partitioning**: `RemotePartitioningManagerStepBuilder` splits 4.2M beneficiaries into 20 parallel workers
- **JPA ItemWriter**: Bulk insert via `JpaItemWriter` with JDBC batch (50 records at once) — 10x faster than single INSERT
- **Monitoring**: Spring Batch Admin / Actuator exposes `/batch/jobs` endpoint for real-time batch progress monitoring
- **Retry/Skip**: Configurable skip limit for individual beneficiary errors without aborting entire batch

---

## 📊 Consequences

### Positive

- **SLA achievement**: Partitioned processing reduces BATCHPGT from 1.5-3.3h to <15 minutes for 4.2M beneficiaries
- **Operator experience**: Real-time API path allows 500+ concurrent users without waiting for batch
- **Resilience**: Batch failures don't impact real-time API availability (separate execution contexts)
- **Banking compliance**: CNAB 240 batch file generation preserved exactly as legacy behavior

### Negative / Trade-offs

- **Two code paths**: Batch and real-time share repositories but use different orchestration; developers must understand both
- **Database contention**: Batch INSERT of 3.8M records competes with real-time queries; mitigated by batch running at 22:00h and read replicas
- **Complexity**: Spring Batch configuration (Steps, Chunks, Partitions, JobRepository) has learning curve

---

## 🔀 Alternatives Considered

### Alternative A: Full Real-Time via Event-Driven (Kafka)
- **Pros**: Loose coupling, natural audit trail via event log, scalable
- **Rejected because**: Banking CNAB 240 standard requires batch files; Kafka infrastructure complexity; payments must complete by bank deadline (not eventually)

### Alternative B: Quartz Scheduler (replace Spring Batch)
- **Pros**: Simpler than Spring Batch for scheduled jobs
- **Rejected because**: No built-in chunk processing, restart-from-checkpoint, or partitioning; would require reimplementing what Spring Batch provides

### Alternative C: Azure Logic Apps / Data Factory
- **Pros**: No-code/low-code, Azure-native
- **Rejected because**: Too low-level control for complex discount calculation logic; debugging Complex business rules in Logic Apps is difficult; Java is preferred for business logic

---

## 🛡️ Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Batch job failure mid-run | MEDIUM | HIGH | Spring Batch restart-from-checkpoint; skip limit for per-beneficiary errors; alerting via Spring Actuator |
| Database contention (batch INSERT vs. API reads) | MEDIUM | MEDIUM | Schedule batch at 22:00h low-traffic; PostgreSQL read replica for real-time APIs |
| Batch SLA missed (>1h) | LOW | MEDIUM | Monitoring with 50-minute alert threshold; horizontal scaling of partitions available |
| Partial generation (batch aborted at 70%) | LOW | HIGH | Transactional chunks; incomplete batches automatically rolled back; restart-from-checkpoint |

---

## 🔗 References

- [Spring Batch 5.x Partitioning Guide](https://docs.spring.io/spring-batch/reference/scalability.html)
- [CNAB 240 Brazilian Banking Standard](https://www.febraban.org.br/associados/tecnologias/cnab240/)
- [BATCHPGT.NSN legacy analysis](../legacy/natural-programs/BATCHPGT.NSN)
- [REQ-PERF-001 (Batch <1h SLA)](../SPECIFICATION.md#req-perf-001)

---

## ✅ Approval

| Role | Name | Status |
|---|---|---|
| Backend Developer | Danilo Lisboa | ✅ Approved |
| Tech Lead | Vitor Filincowsky | ✅ Approved |
| DevOps Engineer | Anne Caselato | ✅ Approved |
