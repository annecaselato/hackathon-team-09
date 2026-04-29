---
title: "ADR-004: Modular Monolith over Microservices"
status: "Accepted"
date: "2026-04-29"
deciders: ["Vitor Filincowsky (Tech Lead)", "Enterprise Architect", "Anne Caselato (DevOps)"]
tags: ["stage-2", "adr", "architecture", "modular-monolith", "microservices"]
---

# ADR-004: Modular Monolith over Microservices

**Status**: Accepted  
**Date**: 2026-04-29  
**Deciders**: Vitor Filincowsky (Tech Lead), Enterprise Architect, Anne Caselato (DevOps)

---

## 📖 Context

SIFAP 2.0 must process payment workflows that span multiple domains:

```
CADBENEF → VALBENEF → BATCHPGT → CALCBENF → CALCDSCT → PAGAMENTO → AUDITORIA
```

The team is evaluating architectural patterns for the Java backend. Options are:
1. **Microservices**: Each domain (beneficiary, payment, audit) as an independent service
2. **Modular Monolith**: Single deployable unit with internal module boundaries
3. **Traditional Monolith**: No internal boundaries, single codebase

Team size: 6 engineers (1 Tech Lead, 2 Backend, 2 Frontend, 1 DevOps). Timeline: hackathon-constrained.

**Key concern from Stage 1**: Payment calculation touches 3 entities (BENEFICIARIO, PAGAMENTO, PROGRAMA-SOCIAL, AUDITORIA) in a single batch transaction — splitting these into separate services would require distributed transactions.

---

## ⚖️ Decision

We will implement SIFAP 2.0 as a **Modular Monolith**: a single deployable JAR with explicitly defined internal module boundaries (packages by feature/domain), using Java 21 module system conventions.

**Package Structure**:
```
com.datacorp.sifap
├── beneficiary/          # CADBENEF, VALBENEF equivalent
│   ├── BeneficiaryController
│   ├── BeneficiaryService
│   ├── BeneficiaryRepository
│   └── BeneficiaryValidator
├── payment/              # BATCHPGT, BATCHCON, BATCHREL equivalent
│   ├── PaymentBatchJob
│   ├── PaymentService
│   ├── PaymentRepository
│   └── DiscountCalculator
├── calculation/          # CALCBENF, CALCDSCT, CALCCORR equivalent
│   ├── BenefitCalculationService
│   ├── DiscountCeilingPolicy
│   └── DecemberBonusPolicy
├── audit/                # RELAUDIT, RELPGT equivalent
│   ├── AuditService
│   └── AuditRepository
└── program/              # PROGRAMA-SOCIAL equivalent
    ├── ProgramService
    └── ProgramRepository
```

**Module Contract Rules**:
- Modules communicate only through **public service interfaces** (no direct repository access across modules)
- No circular dependencies between modules (enforced by ArchUnit tests)
- Each module owns its database tables (no foreign keys across module boundaries)

---

## 💡 Rationale

### Why NOT Microservices (Yet)

- **Distributed transactions**: BATCHPGT creates PAGAMENTO records while reading BENEFICIARIO and PROGRAMA-SOCIAL; in microservices, this requires Saga pattern or 2PC — high complexity for current team size
- **Team size**: 6 engineers. Conway's Law: architecture mirrors team structure. Microservices optimal for 3+ teams; modular monolith optimal for 1-2 teams
- **Timeline**: Microservices require 30-50% extra infrastructure (service discovery, service mesh, distributed tracing, API gateways); not feasible for hackathon timeline
- **Operational complexity**: 6-8 services requires Kubernetes orchestration, circuit breakers, distributed logging; 1 deployment is simpler
- **Performance**: Inter-service HTTP calls add 10-50ms latency per hop; payment calculation loop (4.2M iterations) would be impractical

### Why Modular Monolith IS the Right Choice

- **Extract-ready**: Well-bounded modules can be extracted to microservices later (Strangler Fig Pattern) when teams grow
- **ACID transactions**: Database transactions span all modules freely — no Saga complexity for batch jobs
- **Simple deployment**: Single Docker container → Azure App Service; no Kubernetes required
- **Debuggability**: Single process, single log file, single distributed trace — much easier for team to debug
- **Future path**: Module boundaries defined today become service boundaries tomorrow when team scales

### Strangler Fig Migration Path

```
Phase 1 (Now): Modular Monolith
  └─ All modules in one JAR
  
Phase 2 (Optional, 12-24 months): Extract high-scale modules
  ├─ payment-service (separate deployment - high batch volume)
  └─ audit-service (separate deployment - compliance/immutability SLA)
  
Phase 3 (Optional, 24+ months): Full microservices if needed
  └─ Driven by team growth, not architecture dogma
```

---

## 📊 Consequences

### Positive

- **Simplicity**: Single codebase, single deployment, single database schema
- **Refactorability**: Modules can be restructured without breaking network contracts
- **ACID transactions**: No distributed transaction complexity for batch payment processing
- **Developer velocity**: Single IDE session; all code accessible without context switching
- **Test isolation**: Module boundaries enforced by ArchUnit; easy to test individual modules

### Negative / Trade-offs

- **Scaling granularity**: Cannot scale only the "payment" module independently; must scale entire service. Mitigated by Azure scale-up (vertical scaling) and batch parallelism
- **Long-term**: If team grows to 20+ engineers, modular monolith becomes constraint. Mitigation: Strangler Fig extraction path defined today
- **Shared database**: All modules share PostgreSQL instance; table-level isolation via ownership rules

---

## 🔀 Alternatives Considered

### Alternative A: Microservices from Day 1
- **Pros**: Independent scaling, team autonomy, technology flexibility per service
- **Rejected because**: Premature for team size; distributed transaction complexity; hackathon timeline

### Alternative B: Traditional Monolith (no module boundaries)
- **Pros**: Fastest initial development
- **Rejected because**: Long-term maintainability; makes future microservices extraction impossible; no enforcement of business domain boundaries

### Alternative C: Event-Driven Microservices with Kafka
- **Pros**: Loose coupling, eventual consistency, audit trail via event log
- **Rejected because**: Introduces Kafka infrastructure complexity; eventual consistency is incompatible with Lei 8159 (must have immediate audit records); team not familiar

---

## 🛡️ Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Module coupling creep over time | MEDIUM | MEDIUM | ArchUnit tests fail on circular dependencies; PR reviews enforce module contract |
| Monolith too large to deploy | LOW | LOW | Current scope is 5 modules; modular structure keeps JAR <200MB |
| Cannot scale payment batch independently | MEDIUM | MEDIUM | Spring Batch with remote partitioning allows scale-out without microservices |

---

## 🔗 References

- [Sam Newman — Building Microservices (Ch.13: Decomposition)](https://samnewman.io/books/building_microservices_2nd_edition/)
- [Martin Fowler — Modular Monolith](https://martinfowler.com/articles/modularmonolith.html)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [ArchUnit Testing for Java](https://www.archunit.org/)

---

## ✅ Approval

| Role | Name | Status |
|---|---|---|
| Tech Lead | Vitor Filincowsky | ✅ Approved |
| DevOps Engineer | Anne Caselato | ✅ Approved |
| Enterprise Architect | (Stage 2 review) | 🔄 Pending |
