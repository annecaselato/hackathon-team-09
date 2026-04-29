---
title: "Mysteries Checklist"
description: "Unresolved questions from Stage 1 legacy code exploration"
author: "Paula Silva, AI-Native Software Engineer, Americas Global Black Belt at Microsoft"
date: "2026-04-29"
version: "1.2.0"
status: "approved"
tags: ["stage-1", "mysteries", "questions", "investigation"]
---

# 🔍 Open Mysteries — SIFAP Legacy Archaeology

> Track unknowns discovered during code archaeology. Resolve before Stage 2 (specification) begins. Categorize by severity.

---

## 📑 Table of Contents

1. [🔍 Critical Mysteries (Blocks Stage 2)](#critical-mysteries-blocks-stage-2)
2. [🔍 Important Mysteries (Should Clarify Before Stage 3)](#important-mysteries-should-clarify-before-stage-3)
3. [🔍 Nice-to-Know Mysteries (Can Defer Until After Go-Live)](#nice-to-know-mysteries-can-defer-until-after-go-live)
4. [📋 Resolution Tracking](#resolution-tracking)
5. [🔬 Investigation Methods](#investigation-methods)
6. [➡️ Next Steps](#next-steps)

---

---

## 🔍 Critical Mysteries (Blocks Stage 2)

These questions must be answered before architecture decisions can be made.

### M-001: Payment Record Archival Strategy

**Question**: How are payment records older than 7 years archived? Are they physically deleted from PAYMENT.DDM or moved to archive tables?

**Why it matters**: Impacts database schema design, backup strategy, and data migration approach for modernization.

**Impact**: HIGH (Must finalize retention policy)

**Discovered by**: Archaeologist Agent

**Investigation status**: ✅ RESOLVED
- [x] Analyzed legacy/adabas-ddms/PAGAMENTO.ddm
- [x] Confirmed no archival/purge policy in 28 years of operation
- [x] Verified compliance requirement (Lei 8159, 10-year minimum)

**Resolution**: All payment records retained since 1998. Growth: 3.8M/month, 180M+ total. Modernization must handle full historical load.

---

### M-002: Judicial Discount Approval Workflow

**Question**: Is there an approval/authorization step before judicial discounts are applied to payments?

**Why it matters**: Need to understand approval SLA and reversal process post-bank dispatch.

**Impact**: HIGH (Compliance & financial controls)

**Discovered by**: Archaeologist Agent

**Status quo**: [legacy/natural-programs/CALCDSCT.NSN](legacy/natural-programs/CALCDSCT.NSN#L131-L165) shows automatic application, no approval step in code.

**Investigation status**: 🟡 PARTIALLY RESOLVED
- [x] Judicial discounts auto-applied in batch (no approval in CALCDSCT)
- [x] External approval process exists (judicial order issued outside SIFAP)
- [ ] Reversal/cancellation workflow post-bank dispatch (still OPEN)
- [ ] Upstream approval SLA documentation

**Resolution**: Judicial orders approved externally, then manually entered into system. No reversal logic found. Modernization must clarify compensation vs. reversal approach.

---

### M-003: Discount Priority Order

**Question**: When a beneficiary has multiple discount types (judicial + CPMF + income tax), what is the order of application?

**Why it matters**: Affects net amount calculation if rounding or ceiling constraints apply.

**Impact**: MEDIUM (Calculation correctness)

**Discovered by**: Archaeologist Agent

**Investigation status**: 🟡 PARTIALLY RESOLVED
- [x] Analyzed [legacy/natural-programs/CALCDSCT.NSN](legacy/natural-programs/CALCDSCT.NSN#L109-L178) loop logic
- [ ] No hardcoded priority found (insertion order used)
- [ ] Need business confirmation: is priority required?

**Resolution**: Discounts applied sequentially in GRP-DESCONTO array order. No documented priority rule. Modernization must confirm if insertion order is acceptable.

---

### M-004: 13th Month Proportionality

**Question**: Is the 13th month bonus proportional to months worked, or full amount regardless?

**Why it matters**: Code comment mentions proportionality but implementation unclear.

**Impact**: MEDIUM (Calculation correctness)

**Discovered by**: Archaeologist Agent

**Investigation status**: ✅ RESOLVED with CAVEAT
- [x] Analyzed [legacy/natural-programs/CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L243-L257)
- [x] Code shows NO proportionality divisor (comment may be obsolete)
- [ ] Need business confirmation: is legacy calculation correct or incorrect?

**Resolution**: Actual formula grants full 13º without month divisor. Comment at line 241 suggests proportionality (meses_ativos/12) but code lacks this. **CAVEAT**: Modernization must verify if legacy calculation is a bug or intentional policy.

---

## 🔍 Important Mysteries (Should Clarify Before Stage 3)

These should be resolved to avoid surprises during implementation.

### M-005: Payment Reversal After Bank Dispatch

**Question**: What is the process to reverse/cancel a payment after it has been dispatched to the bank?

**Why it matters**: Need reversal logic for chargebacks, fraud, or correction scenarios.

**Impact**: MEDIUM (Missing functionality)

**Discovered by**: Archaeologist Agent

**Investigation status**: 🟡 PARTIALLY RESOLVED
- [x] Analyzed [legacy/natural-programs/CALCCORR.NSN](legacy/natural-programs/CALCCORR.NSN#L128-L162)
- [x] CALCCORR can update existing PAGAMENTO fields (value, discount)
- [ ] Status transitions post-bank-dispatch (P status) unclear
- [ ] Compensation vs. reversal approach not documented

**Resolution**: UPDATE logic exists but post-dispatch reversal SLA unclear. Modernization must clarify: Is compensation transaction created or is original reversed?

---

### M-006: 3270 vs. X Client Interface Choice

**Question**: Why was 3270 terminal interface chosen over X Client (modern web)?

**Why it matters**: Affects UX strategy during modernization.

**Impact**: LOW (Historical context)

**Discovered by**: Archaeologist Agent

**Investigation status**: ✅ RESOLVED
- [x] DATACORP architecture standard: 3270 for legacy/mainframe phase
- [x] X client introduced post-Stage-3 (modern Java/Spring stack)
- [x] Com*plete 6.1.2 teleprocessing monitor mandates 3270 during transition

**Resolution**: Standard choice, not a defect. Modernization will introduce modern web UI in final stage.

---

### M-007: Batch Performance & Scale Limits

**Question**: At what data volume does BATCHPGT performance degrade beyond acceptable SLA (1.5-3.3h window)?

**Why it matters**: Plan capacity & partitioning strategy for modernized system.

**Impact**: MEDIUM (Non-functional requirements)

**Discovered by**: Archaeologist Agent

**Investigation status**: 🟡 PARTIALLY RESOLVED
- [x] Current batch: BATCHPGT 1.5-3.3h, BATCHCON varies
- [x] Current data: 180M+ PAGAMENTO records, 3.8M/month growth
- [ ] Saturation point & scaling model not documented
- [ ] Sequential read performance degradation curve unknown

**Resolution**: Current performance acceptable on mainframe. Modern PostgreSQL must plan for partitioning (by year-month or CPF prefix) to maintain <1h SLA.
- [ ] Query PAYMENT.DDM for max amounts historically
- [ ] Ask fraud prevention team

**Resolution**: [Pending]

---

### M-007: Payment Reversal After Finance Dispatch

**Question**: If a payment has been sent to Finance system but an error is later discovered (e.g., wrong CPF), what is the reversal process? Can legacy SIFAP reverse it, or is it manual?

**Why it matters**: Modern system should have clear reversal workflow.

**Impact**: MEDIUM (Error handling)

**Discovered by**: [Team member]

**Investigation status**: OPEN
- [ ] Check if CALCPAY has reversal logic
- [ ] Ask Finance team about reconciliation process
- [ ] Review audit logs for reversal examples

**Resolution**: [Pending]

---

### M-008: CPF Validation External Service

**Question**: Does SIFAP call an external CPF validation service (e.g., Federal Tax Authority API), or is validation purely algorithmic (modulo-11)?

**Why it matters**: Modern system may need to replicate external call or replace with API.

**Impact**: MEDIUM (External dependency)

**Discovered by**: [Team member]

**Investigation status**: OPEN
- [ ] Check VALIDATE-CPF for external calls
- [ ] Look for network/API logs
- [ ] Ask legacy admin about external integrations

**Resolution**: [Pending]

**Current finding**: Appears to be local (modulo-11 only), but needs confirmation.

---

## 🔍 Nice-to-Know Mysteries (Can Defer Until After Go-Live)

These are interesting but not blocking.

### M-009: Historical System Evolution

**Question**: Why was Natural/Adabas chosen in 2015? Were there other options considered?

**Why it matters**: Historical context; helps understand architectural decisions.

**Impact**: LOW (Historical interest)

**Status**: Deferred (post-go-live)

---

### M-010: Performance Characteristics at Scale

**Question**: Have there been performance issues when beneficiary count exceeded 1M? What was the response time degradation?

**Why it matters**: Helps inform capacity planning for modern system.

**Impact**: LOW (Performance tuning)

**Status**: Deferred (post-go-live)

**Investigation approach**: Review historical load test reports if available.

---

### M-011: Batch Job Failure Recovery

**Question**: If the nightly batch (NIGHTLY-BATCH) fails mid-execution, what is the recovery process? Does it resume from checkpoint or restart?

**Why it matters**: Modern system should have similar resilience.

**Impact**: LOW (Operational knowledge)

**Status**: Deferred (post-go-live)

---

## 📋 Resolution Tracking

| Mystery ID | Status | Resolved By | Resolution Date | Answer |
|---|---|---|---|---|
| M-001 | OPEN | TBD | TBD | |
| M-002 | OPEN | TBD | TBD | |
| M-003 | OPEN | TBD | TBD | |
| M-004 | OPEN | TBD | TBD | |
| M-005 | OPEN | TBD | TBD | |
| M-006 | OPEN | TBD | TBD | |
| M-007 | OPEN | TBD | TBD | |
| M-008 | OPEN | TBD | TBD | |
| M-009 | DEFERRED | TBD | TBD | |
| M-010 | DEFERRED | TBD | TBD | |
| M-011 | DEFERRED | TBD | TBD | |

---

## 🔬 Investigation Methods

### Method 1: Code Reading

Read Natural programs, check for patterns and calls.

**Tools**: Text editor, GitHub Copilot Chat

**Time**: 15-30 minutes per program

---

### Method 2: Data Query

Query Adabas DDM samples to understand real-world variations.

**Tools**: SQL client (if DDM can be queried) or raw file inspection

**Time**: 10-20 minutes per query

---

### Method 3: Stakeholder Interview

Ask business owners or legacy admin directly.

**Stakeholders**: Operations team, auditor, legacy system admin

**Time**: 20-30 minutes per interview

---

### Method 4: Historical Audit Log Analysis

Review AUDIT.DDM for patterns of operations or edge cases.

**Tools**: SQL or report generation

**Time**: 30-60 minutes

---

## ➡️ Next Steps

1. **Assign investigators** to each critical mystery (M-001 through M-008)
2. **Schedule interviews** with legacy admin and business stakeholders
3. **Target resolution date**: Before Stage 2 kickoff (28/04 afternoon)
4. **Document findings** in discovery-report.md
5. **Update SPECIFICATION.md** with resolved business rules

---

| Previous | Home | Next |
|:---------|:----:|-----:|
| [← Discovery Report](discovery-report.md) | [Kit Home](../README.md) | [Mysteries Found →](mysteries-found.md) |
