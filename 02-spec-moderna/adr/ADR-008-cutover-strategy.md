---
title: "ADR-008: Phased Cutover Strategy (Strangler Fig)"
status: "Accepted"
date: "2026-04-29"
deciders: ["Vitor Filincowsky (Tech Lead)", "Anne Caselato (DevOps)", "Product Owner"]
tags: ["stage-2", "adr", "cutover", "migration", "strangler-fig", "rollback"]
---

# ADR-008: Phased Cutover Strategy (Strangler Fig)

**Status**: Accepted  
**Date**: 2026-04-29  
**Deciders**: Vitor Filincowsky (Tech Lead), Anne Caselato (DevOps), Product Owner

---

## 📖 Context

SIFAP legacy system processes:
- **4.2M active beneficiaries** per monthly payment cycle
- **3.8M payment records/month** generation via BATCHPGT
- **25M+ immutable audit records** (compliance requirement)
- **Legal obligation**: Payments must be processed on the 1st business day of each month without fail

**Cutover risks**:
- A failed cutover on payment day means beneficiaries don't receive payments — regulatory violation
- If modern system has a bug in payment calculation, wrong amounts could be paid to 4.2M people
- Rolling back after bank dispatch is complex (requires reversal process — open mystery M-005)

**Cutover approach must**:
- Allow validation of business rule correctness before full cutover
- Provide rollback capability in <4 hours if critical issues detected
- Not disrupt monthly payment cycles
- Satisfy compliance teams that no data gap exists during transition

---

## ⚖️ Decision

We will use the **Strangler Fig Pattern** — gradual replacement of legacy by routing increasing percentages of traffic to the modern system, with the legacy system running in parallel as fallback.

### Cutover Phases

#### Phase 0: Shadow Mode (Before go-live, 1 month)
- Modern system receives **copies** of all operations (dual-write)
- Modern system processes payments for 0% of beneficiaries (shadow only)
- Reconciliation dashboard compares legacy vs. modern calculations daily
- **Goal**: Validate payment calculations match legacy output exactly

**Verification Criteria**:
- Payment amounts match legacy for 100% of test beneficiaries (random sample of 10,000)
- Audit records generated correctly for all operations
- API response times <500ms p99 for beneficiary queries

#### Phase 1: 5% Canary (Month 1 post go-live)
- Route **5% of beneficiaries** (~210,000) to modern system
- Selection: Random subset by CPF hash (deterministic, not arbitrary)
- Legacy processes remaining 95% of beneficiaries as usual
- **Monitoring**: Alert if modern batch SLA >1h OR any payment amount mismatch detected

**Go/No-Go Criteria**:
- Zero payment calculation discrepancies vs. expected values
- Batch BATCHPGT completes in <30 minutes for 210,000 beneficiaries
- Zero critical bug reports from operators

#### Phase 2: 25% Expansion (Month 2 post go-live)
- Route **25% of beneficiaries** (~1,050,000) to modern system
- Scale-up PostgreSQL instance before this phase
- **Monitoring**: Spring Batch metrics, p99 API response, error rates

**Go/No-Go Criteria**:
- No performance degradation (batch <45 minutes for 1.05M)
- Operator satisfaction survey >80% positive (web UI vs. 3270)
- Zero compliance audit findings

#### Phase 3: 100% Full Cutover (Month 3 post go-live)
- Route **all beneficiaries** to modern system
- Legacy SIFAP set to **read-only mode** (no new writes)
- Operators use SIFAP 2.0 web UI exclusively
- **Legacy accessible for 90 days** as read-only compliance reference

#### Phase 4: Legacy Decommission (Month 6 post go-live)
- After 90-day read-only period, legacy mainframe decommissioned
- All audit records verified in PostgreSQL
- Adabas data final backup archived to Azure Blob Storage (cold tier, 10-year retention)

### Traffic Routing

```
┌──────────────────────────────────────────┐
│              Azure Front Door            │
│         (or Spring Cloud Gateway)        │
│                                          │
│  Route by CPF hash modulo 100:           │
│  ├─ 0-4 → SIFAP 2.0 (Phase 1: 5%)       │
│  ├─ 0-24 → SIFAP 2.0 (Phase 2: 25%)     │
│  └─ 0-99 → SIFAP 2.0 (Phase 3: 100%)    │
│                                          │
│  Remaining % → Legacy SIFAP (mainframe)  │
└──────────────────────────────────────────┘
```

### Rollback Plan

| Scenario | Rollback Action | RTO |
|---|---|---|
| Critical bug in payment calculation | Route all traffic back to legacy | <1 hour |
| Batch job failure (BATCHPGT) | Restart on legacy; mark modern records as cancelled | <2 hours |
| Data integrity issue detected | Halt modern; run legacy reconciliation report | <4 hours |
| Database corruption | Restore from latest backup; replay delta from legacy | <6 hours |

---

## 💡 Rationale

### Why Strangler Fig (not Big Bang)

- **Risk mitigation**: 4.2M beneficiaries depending on correct payments; any error affects millions
- **Incremental confidence**: Each phase validates correctness before expanding; Phase 1 failure affects 210K (5%), not 4.2M (100%)
- **Business continuity**: Monthly payment cycles are sacrosanct; cutover timed to **avoid payment generation week**
- **Compliance coverage**: Parallel run provides complete audit trail with no gap

### Why CPF Hash for Routing

- **Deterministic**: Same beneficiary always routes to same system (modern or legacy); avoids payment duplication
- **No configuration per beneficiary**: Hash-based routing requires no per-record flags; scales automatically
- **Gradual expansion**: Changing modulo threshold from 5 → 25 → 100 is single config change with immediate effect

### Why 90-Day Read-Only Legacy Retention

- Compliance teams require access to historical 3270 screens for ongoing audit work
- Bank reconciliation for pre-cutover payments may require legacy reference
- Time-boxed at 90 days to drive urgency of full adoption

---

## 📊 Consequences

### Positive

- **Risk reduction**: Each phase validates 5 → 25 → 100% before committing to full cutover
- **Rollback capability**: Legacy available for 90 days; production rollback in <4 hours
- **No payment disruption**: Phased cutover avoids processing payment day under full load immediately
- **Team learning curve**: Operators transition gradually to new web UI

### Negative / Trade-offs

- **Dual system complexity**: 3 months of parallel operation; dual-write adds ETL/CDC complexity (see ADR-006)
- **Reconciliation overhead**: Comparing legacy and modern output daily requires dedicated reconciliation service
- **Extended legacy costs**: Mainframe licensing continues for 3+ months during parallel run

---

## 🔀 Alternatives Considered

### Alternative A: Big Bang Cutover (single weekend)
- **Pros**: Clean; eliminates dual-system complexity immediately
- **Rejected because**: 4.2M beneficiaries at risk; no validation period; regulatory violation if payments fail; rollback from big bang is extremely complex

### Alternative B: Feature Flags per Beneficiary
- **Pros**: Fine-grained control; can target specific beneficiary groups
- **Rejected because**: Requires flag management for 4.2M records; CPF hash is equivalent with less overhead

### Alternative C: Blue/Green Deployment
- **Pros**: Instant switch; full rollback possible
- **Rejected because**: Requires running two full environments simultaneously (cost); no gradual validation phase; BATCHPGT can't run in both environments simultaneously without duplicate payments

---

## 🛡️ Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Wrong payment amount in Phase 1 | LOW | HIGH | Shadow mode validation before Phase 1; alerts on amount deviation >0.01 |
| Beneficiary CPF routing duplication | LOW | CRITICAL | Hash is deterministic; idempotency key on PAGAMENTO prevents duplicates (REQ-PAY-001) |
| Monthly payment deadline missed | LOW | CRITICAL | Phases aligned to exclude 1st-business-day window; cutover only after monthly cycle completes |
| Legacy decommission blocked by compliance | MEDIUM | MEDIUM | Time-box at 90 days; compliance review mandatory before extending |

---

## 🔗 References

- [Martin Fowler — Strangler Fig Application](https://martinfowler.com/bliki/StranglerFigApplication.html)
- [ADR-003 (PostgreSQL Migration)](./ADR-003-postgresql-migration.md)
- [ADR-006 (Data Migration Strategy)](./ADR-006-data-migration-strategy.md)
- [REQ-DATA-001 (No Purge Policy)](../SPECIFICATION.md#req-data-001)
- [M-005 (Payment reversal after dispatch — open mystery)](../01-arqueologia/mysteries-checklist.md)

---

## ✅ Approval

| Role | Name | Status |
|---|---|---|
| Tech Lead | Vitor Filincowsky | ✅ Approved |
| DevOps Engineer | Anne Caselato | ✅ Approved |
| Product Owner | (Stage 2 review) | 🔄 Pending |
