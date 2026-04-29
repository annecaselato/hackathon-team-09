---
title: "Discovery Report"
description: "Comprehensive findings from SIFAP legacy system exploration"
author: "Paula Silva, AI-Native Software Engineer, Americas Global Black Belt at Microsoft"
date: "2026-04-29"
version: "1.2.0"
status: "approved"
tags: ["stage-1", "discovery", "report", "findings"]
---

# 📊 SIFAP Legacy System Discovery Report

> Summary of all findings from Stage 1 archaeology including system overview, business rules, risks, and recommendations.

---

## 📑 Table of Contents

1. [📊 Executive Summary](#executive-summary)
2. [🏗️ System Overview](#system-overview)
3. [🔄 Business Processes](#business-processes)
4. [📦 Data Structure and Volume](#data-structure-and-volume)
5. [📊 Business Rules Summary](#business-rules-summary)
6. [⚠️ Known Issues and Limitations](#known-issues-and-limitations)
7. [🔍 Mysteries (Open Questions)](#mysteries-open-questions)
8. [⚠️ Risks and Mitigations](#risks-and-mitigations)
9. [💡 Recommendations](#recommendations)
10. [🎯 Conclusion](#conclusion)
11. [📎 Appendices](#appendices)

---

---

## 📊 Executive Summary

The legacy SIFAP system has been in production since 2015, managing benefit payments for thousands of beneficiaries. The system is built on Natural/Adabas technology and has accumulated significant business logic over 11 years. While the core functionality is stable, modernization is necessary to improve user experience, reduce operational costs, and enable faster feature development.

**Key findings**:
- 16 core Natural programs (CADBENEF, VALBENEF, CALCBENF, CALCDSCT, CALCCORR, BATCHPGT, BATCHREL, BATCHCON, CONSBENF, RELAUDIT, RELPGT, VALDOCS, VALELEG, CADDEPEND, CADPROG, others)
- 14 documented business rules with line-level traceability
- 4 Adabas DDMs (BENEFICIARIO, PAGAMENTO, AUDITORIA, PROGRAMA-SOCIAL)
- Monthly batch processing: BATCHPGT 1.5-3.3h, BATCHCON variable
- 180M+ payment records, 3.8M/month growth, no purge policy (28 years retained)
- 100% traceability through immutable audit tables (25M+ records since 2005)

---

## 🏗️ System Overview

### Purpose

SIFAP (Sistema de Benefícios Sociais - Social Benefits Payment System) is a government benefit management system managing payments for thousands of beneficiaries. It handles:

1. **Beneficiary Registration & Validation**: Register and validate individuals eligible for government benefits
2. **Benefit Calculation**: Calculate monthly benefit payments with factors (regional, family, income, age) and apply discounts
3. **Payment Processing**: Generate batch payment files, reconcile with bank returns
4. **Audit and Compliance**: Maintain immutable audit trail for all operations (Lei 8159, 10-year retention minimum)

### Users and Roles

| Role | Usage | Tools |
|------|-------|-------|
| Operator | Register beneficiaries, process corrections | 3270 terminal (Com*plete 6.1.2) |
| Batch Admin | Monitor nightly BATCHPGT, BATCHCON execution | Mainframe job scheduling, logs |
| Auditor | Review operations via RELAUDIT, RELPGT | 3270 terminal + batch PDF reports |
| Finance | Reconciliation and oversight | Bank CNAB 240 files, PDF reports |

### Current Technology Stack

| Component | Technology | Version | Status |
|-----------|-----------|---------|--------|
| Language | Natural | 6.3.12 | End-of-life |
| Database | Adabas | 7.4.3 | Expensive maintenance |
| Teleprocessing | Com*plete | 6.1.2 | Obsolete |
| Interface | 3270 Terminal | MVS/ESA | Obsolete |
| Reports | PDF (batch) | Natural Report Writer | Manual generation |
| Deployment | IBM Mainframe | MVS/ESA | High operational cost |

---

## 🔄 Business Processes

### Process 1: Beneficiary Lifecycle

```
REGISTER (CADBENEF)
  ├─ VALBENEF: Validate CPF (modulo-11), name, date, UF, status
  ├─ Check for duplicates in BENEFICIARIO
  ├─ Set initial status = A (ACTIVE)
  └─ Create audit record (COD-ACAO=IN)
  
UPDATE (CADBENEF)
  ├─ Modify fields (NOME, NASCIMENTO, UF, etc.)
  ├─ Status transitions: A → S → C (cannot reverse)
  └─ Create audit record (COD-ACAO=AL)
  
CONSULT (CONSBENF)
  ├─ Query BENEFICIARIO by CPF filter
  └─ Display current status and linked discounts
```

### Process 2: Monthly Payment Cycle (BATCHPGT)

```
TRIGGER: 1st business day of month, 22:00h
  |
  v
For each BENEFICIARIO where STATUS=A
  |
  +---> [Payment exists for ANO-MES-REF?] → SKIP (duplicate check)
  |
  v
Retrieve PROGRAMA-SOCIAL base amount & parameters
  |
  v
CALCBENF: Calculate VLR-BRUTO
  ├─ base × fator-regional × fator-familia × fator-renda × fator-idade × (1 + reajuste)
  ├─ [IF MONTH=12: add 13º = BASE × fator-regional × fator-idade]
  └─ [IF PROGRAMA=A & MONTH=12: add abono 15% of regular]
  |
  v
CALCDSCT: Apply discounts, enforce ceiling
  ├─ For each TIPO-DESCONTO in GRP-DESCONTO
  ├─ IF TIPO-DESCONTO ≠ 'J' AND total > 30% → TRUNCATE to 30%
  ├─ IF TIPO-DESCONTO = 'J' → No ceiling
  └─ Return VLR-DESCONTO
  |
  v
Calculate VLR-LIQUIDO = VLR-BRUTO - VLR-DESCONTO
  |
  v
STORE PAGAMENTO
  ├─ NUM-PAGAMENTO (auto-increment)
  ├─ Status = G (gerado/generated)
  ├─ DT-GERACAO = current timestamp
  └─ Create audit record (COD-ACAO=IN, TIPO-ENTIDADE=PGTO)
  |
  v
SUMMARY LOG
  └─ Total payments generated, total amounts, processing time
```

### Process 3: Bank Reconciliation (BATCHCON)

```
TRIGGER: Monthly, after payment dispatch (typically 1-2 days after BATCHPGT)
  |
  INPUT: Bank return file (CNAB 240 format)
  |
  v
For each return record
  |
  +---> FIND PAGAMENTO by NUM-PAGTO (descriptor lookup)
  |
  +---> [Payment not found?] → Log divergence (REC-004)
  |
  +---> [Amount mismatch?] → Log divergence (REC-005)
  |
  v
UPDATE PAGAMENTO
  ├─ Status: G → P (pagamento/paid) or D (devolvido/returned)
  ├─ DT-CONFIRMACAO = bank confirmation date
  └─ Create audit record (COD-ACAO=CO, relationship tracking)
  |
  v
AUDITORIA logging
  └─ Divergences tracked for dispute resolution
  |
  v
SUMMARY LOG
  └─ Total confirmed, total divergences, processing time
```

### Process 4: Audit and Reporting

```
CONTINUOUS: Immutable audit trail (AUDITORIA DDM)
  ├─ All CREATE operations logged (COD-ACAO=IN)
  ├─ All UPDATE operations logged with old/new values (COD-ACAO=AL)
  ├─ DELETE operations attempted but rejected by constraint
  ├─ Timestamp (TS-EVENTO, UTC), user ID, source program (COD-MODULO)
  └─ Minimum 10-year retention (Lei 8159)

MONTHLY: Generate audit reports
  ├─ RELAUDIT: Filters COD-ACAO (excludes EX = deletions)
  ├─ RELPGT: Payment detail with beneficiary name, amounts, status
  └─ Export as PDF for Finance, Legal, Compliance
```

## 📦 Data Structure and Volume

### Beneficiary Master (BENEFIC DDM)

- **Current records**: ~500,000 active beneficiaries
- **Total records (including cancelled)**: ~1.2 million
- **Growth rate**: 5-10% annually
- **Key fields**: CPF (unique), Status, Benefit Type, Contact Info

### Beneficiary Records (BENEFICIARIO DDM, FNR 151)

- **Count**: ~4.2M beneficiary records (as of Apr 2018)
- **Growth**: Stable (new registrations + cancellations balance)
- **Validation**: CPF (modulo-11), name (requires space), birth date (1900-current), UF (27-state table), status (A/S/C/I/D)
- **Current size**: ~850B average per record

### Payment Records (PAGAMENTO DDM, FNR 152)

- **Monthly volume**: ~3.8M payments (varies by program)
- **Annual volume**: ~45M payments
- **Total records**: 180M+ (no purge since 1998, 28-year history)
- **Retention**: Permanent (compliance requirement: Lei 8159, 10-year minimum)
- **Current size**: ~720B average per record; 180M records × 720B ≈ 130TB+ (compressed ~40TB with backup)
- **Status domain**: P (pending), G (generated), E (error), C (confirmed), D (devolved), X (canceled), R (reversed)
- **Discount breakdown**: Up to 8 discount types per payment (GRP-DESCONTO periodic group)

### Audit Records (AUDITORIA DDM, FNR 153)

- **Count**: ~25M records (since 2005 creation, immutable)
- **Annual entries**: ~2M+ operations (CREATE, UPDATE, DELETE attempts, confirmations)
- **Size**: ~1.2KB average (variable, includes old/new field values)
- **Current size**: ~25M records × 1.2KB ≈ 30GB total
- **Retention**: Permanent (minimum 10 years per Lei 8159)
- **Action codes**: IN (insert), AL (alter), EX (delete attempt), CO (confirm), DV (divergence), etc.
- **Immutability**: Cannot delete or modify audit records (database constraint)

### Social Programs Reference (PROGRAMA-SOCIAL DDM, FNR 154)

- **Count**: ~150 active programs (parameterization table)
- **Fields**: Code, description, base amount, status, effective dates
- **Change frequency**: Quarterly (policy updates)

---

## 📊 Business Rules Summary

### Beneficiary Validation Rules

1. **BR-BEN-001**: CPF must pass modulo-11 algorithm ([VALBENEF.NSN lines 113-236](legacy/natural-programs/VALBENEF.NSN#L113))
2. **BR-BEN-002**: Name must contain space (surname required) ([VALBENEF.NSN lines 264-270](legacy/natural-programs/VALBENEF.NSN#L264))
3. **BR-BEN-003**: Birth date must be valid (1900-current, correct month/day) ([VALBENEF.NSN lines 248-256](legacy/natural-programs/VALBENEF.NSN#L248))
4. **BR-BEN-004**: UF must be in 27-state table ([VALBENEF.NSN lines 145-148](legacy/natural-programs/VALBENEF.NSN#L145))
5. **BR-BEN-005**: Status must be A/S/C/I/D ([VALBENEF.NSN line 164](legacy/natural-programs/VALBENEF.NSN#L164))

### Payment Processing Rules

1. **BR-PAY-001**: No duplicate payment per competencia ([BATCHPGT.NSN lines 202-335](legacy/natural-programs/BATCHPGT.NSN#L202))
2. **BR-PAY-002**: Status domain enforced (P/G/E/C/D/X/R) ([PAGAMENTO.ddm line 48](legacy/adabas-ddms/PAGAMENTO.ddm#L48))
3. **BR-PAY-003**: Non-judicial discounts capped at 30% ([CALCDSCT.NSN lines 101-165](legacy/natural-programs/CALCDSCT.NSN#L101))
4. **BR-PAY-004**: Judicial discounts bypass 30% ceiling ([CALCDSCT.NSN lines 131-165](legacy/natural-programs/CALCDSCT.NSN#L131))

### Calculation Rules

1. **BR-CALC-001**: Regular monthly = base × regional × family × income × age × reajuste ([CALCBENF.NSN lines 225-229](legacy/natural-programs/CALCBENF.NSN#L225))
2. **BR-CALC-002**: December adds 13º = base × regional × age ([CALCBENF.NSN lines 243-249](legacy/natural-programs/CALCBENF.NSN#L243))
3. **BR-CALC-003**: Program A gets 15% abono in December ([CALCBENF.NSN lines 252-257](legacy/natural-programs/CALCBENF.NSN#L252))
4. **BR-CALC-004**: Corrections update existing PAGAMENTO records ([CALCCORR.NSN lines 128-162](legacy/natural-programs/CALCCORR.NSN#L128))

### Audit & Retention Rules

1. **BR-AUD-001**: 10-year minimum retention for AUDITORIA ([AUDITORIA.ddm lines 13-91](legacy/adabas-ddms/AUDITORIA.ddm#L13))
2. **BR-AUD-002**: Audit report excludes EX actions from standard view ([RELAUDIT.NSN line 108](legacy/natural-programs/RELAUDIT.NSN#L108))
3. **BR-DATA-001**: PAGAMENTO has no purge policy (all data since 1998) ([PAGAMENTO.ddm lines 105-106](legacy/adabas-ddms/PAGAMENTO.ddm#L105))

---

## ⚠️ Known Issues and Limitations

### Issue 1: Data Retention Without Archival

**Impact**: Database grows ~3.8M records/month indefinitely

**Root Cause**: No purge policy; compliance requires minimum 10-year retention

**Severity**: MEDIUM (storage cost, performance degradation over time)

**Recommendation for Modern**: Implement partitioning by year-month or CPF prefix in PostgreSQL; archive cold data to blob storage

### Issue 2: Manual Discount Entry for Judicial Orders

**Impact**: Requires manual intervention; prone to data entry errors

**Root Cause**: No automated integration with judicial system

**Recommendation for Modern**: API integration for judicial order ingestion and validation

### Issue 3: Sequential Processing in Batch

**Impact**: BATCHPGT takes 1.5-3.3 hours; cannot parallelize due to Natural/Adabas design

**Root Cause**: Legacy language/database constraints

**Recommendation for Modern**: Implement async/parallel payment processing with Spring Batch or Kafka

### Issue 4: 3270 Terminal Usability

**Impact**: High training cost, slow navigation, accessibility issues

**Root Cause**: Legacy terminal interface designed for 1990s

**Recommendation for Modern**: Web-based UI with modern UX patterns

### Issue 5: No Payment Reversal Workflow

**Impact**: Erroneous payments cannot be reversed easily; only workaround is to adjust next cycle

**Root Cause**: Design does not anticipate post-dispatch corrections

**Recommendation for Modern**: Implement payment reversal/compensation logic with approval workflow

---

## 🔍 Mysteries & Resolution Status

See [mysteries-found.md](mysteries-found.md) for detailed investigation results.

| Mystery ID | Question | Status | Blocker |
|---|---|---|---|
| M-001 | Payment archival strategy? | ✅ RESOLVED: No purge policy since 1998 | No |
| M-002 | Judicial discount approval workflow? | 🟡 PARTIAL: External approval, no reversal found | Yes |
| M-003 | Discount application priority order? | 🟡 PARTIAL: Insertion order, no hardcoded priority | No |
| M-004 | 13th month proportionality? | ✅ RESOLVED with CAVEAT: Code shows no divisor | No |
| M-005 | Payment reversal post-bank dispatch? | 🔴 OPEN: UPDATE logic exists but SLA unclear | Yes |
| M-006 | 3270 vs. X Client choice? | ✅ RESOLVED: DATACORP standard during transition | No |
| M-007 | Batch performance saturation point? | 🟡 PARTIAL: Current SLA 1.5-3.3h acceptable | No |

**Stage 2 Blockers**: M-002 (approval workflow), M-005 (reversal workflow) must be resolved before implementation begins.

---

## ⚠️ Risks and Mitigations

### Risk 1: Business Rule Loss During Modernization

**Severity**: HIGH
**Probability**: MEDIUM

**Mitigation**:
- Every Natural program analyzed and mapped to business rules (BR-*) ✅ DONE (14 rules extracted)
- Every rule traced to source code (line-level references) ✅ DONE
- Every requirement traced to test case (Stage 2)

### Risk 2: Data Loss During Migration

**Severity**: HIGH
**Probability**: LOW

**Mitigation**:
- Parallel run (legacy and modern running side-by-side) for validation
- Reconciliation reports comparing legacy vs. modern payment totals (180M+ records)
- Backup of all Adabas data before migration (encrypted, retained 1 year post-cutover)

### Risk 3: Operator Training

**Severity**: MEDIUM
**Probability**: HIGH

**Mitigation**:
- Early training materials with side-by-side screenshots (old 3270 vs. new web)
- Gradual rollout to small group of operators first
- 24/7 support during cutover week

### Risk 4: Compliance Audit Trail Gap

**Severity**: HIGH
**Probability**: LOW

**Mitigation**:
- Map AUDIT.DDM structure to PostgreSQL audit tables (preserve all fields)
- Verify that every operation in modern system creates audit record
- External audit firm validates audit trail continuity

---

## 💡 Recommendations

### Short-term (Modernization planning)

1. **Validate all 13 documented business rules** with business stakeholders
2. **Resolve mysteries** identified in archaeology (M-001 to M-006)
3. **Estimate data volume** for payment and audit tables (finalize capacity planning)
4. **Interview key operators** about workflow pain points

### Medium-term (Modernization)

1. **Build modern backend** (Java 21 + Spring Boot) with all business rules
2. **Build web UI** (Next.js 15) for beneficiary and payment management
3. **Implement PostgreSQL** with same audit structure as Adabas
4. **Run parallel validation** (legacy SIFAP vs. modern SIFAP side-by-side for 1 full month)

### Long-term (Cutover and optimization)

1. **Phased cutover**: Migrate small beneficiary subset first, then expand
2. **Deprecate legacy** after 3 months of successful modern operation
3. **Optimize for scale**: Modern system should handle 2M beneficiaries comfortably
4. **Enable future features**: Real-time dashboard, mobile app, API for external systems

---

## 🎯 Conclusion

Legacy SIFAP is a stable, compliant system with well-defined business rules. Modernization is justified by:
- Reduced operational cost (move off mainframe)
- Improved user experience (web UI vs. 3270 terminal)
- Faster time-to-market for new features
- Ability to scale beyond current 1.2M beneficiaries

The system is well-understood through archaeology. Stage 2 (specification) can proceed with confidence that all critical business logic has been captured.

---

## 📎 Appendices

### A. File Locations

```
legacy/
├─ Programs/
│  ├─ REGISTBN.NSN    (Beneficiary registration)
│  ├─ CALCPAY.NSN     (Payment calculation)
│  ├─ CALCDSCT.NSN    (Discount application)
│  ├─ GENRPT.NSN      (Report generation)
│  └─ [utility programs]
│
└─ DDM/
   ├─ BENEFIC.DDM
   ├─ PAYMENT.DDM
   ├─ DISCOUNT.DDM
   └─ AUDIT.DDM
```

### B. Key Contact Information

- **Business Owner**: [Name], [Email]
- **Legacy System Admin**: [Name], [Email]
- **Operations Team**: [Email/Slack channel]

### C. References

- System architecture documentation: `legacy/README.md`
- Detailed business rules: `01-arqueologia/business-rules-catalog.md`
- Dependency map: `01-arqueologia/dependency-map.md`
- Domain glossary: `01-arqueologia/glossary.md`

---

| Previous | Home | Next |
|:---------|:----:|-----:|
| [← Dependency Map](dependency-map.md) | [Kit Home](../README.md) | [Mysteries Checklist →](mysteries-checklist.md) |
