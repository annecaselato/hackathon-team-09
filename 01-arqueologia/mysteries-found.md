---
title: "Mysteries Found and Resolved"
description: "Resolutions to mysteries discovered during Stage 1 archaeology"
author: "Paula Silva, AI-Native Software Engineer, Americas Global Black Belt at Microsoft"
date: "2026-04-29"
version: "1.2.0"
status: "approved"
tags: ["stage-1", "mysteries", "resolutions", "answers"]
---

# ✅ Mysteries Found and Resolved — SIFAP Legacy

> As mysteries are investigated and resolved, document the answer here with source and date.

---

## 📑 Table of Contents

1. [📝 Format](#format)
2. [✅ Resolutions](#resolutions)
3. [📊 Summary](#summary)

---

---

## 📝 Format

For each resolved mystery, include:

```markdown
### [Mystery ID]: [Question]

**Resolution**: [Answer]

**Source**: [Where/who did this come from?]

**Date Resolved**: YYYY-MM-DD

**Investigator**: [Team member]

**Details**: [Extended explanation if needed]
```

---

## ✅ Resolutions

### M-001: Payment Record Archival Strategy

**Resolution**: NO PURGE POLICY — All payment records are retained in PAGAMENTO.ddm since system inception (1998). Historical data is never deleted; monthly growth of ~3.8M records (180M+ total as of Apr 2018).

**Source**: [legacy/adabas-ddms/PAGAMENTO.ddm](legacy/adabas-ddms/PAGAMENTO.ddm#L105-L106), system operation logs

**Date Resolved**: 2026-04-29

**Investigator**: Archaeologist Agent (Stage 1)

**Details**: No archival tables exist. Compliance requirement (Lei 8159) mandates 10-year retention minimum. Legacy design chose to keep all data in main table with daily backup strategy. Modernization must plan for full historical load without cutoff.

---

### M-002: Approved Judicial Discount Workflow

**Resolution**: PARTIALLY RESOLVED — Code shows automatic application of judicial discounts (no approval step in CALCDSCT.NSN). External approval process exists outside SIFAP (judicial order issued, manually entered into system). No reversal workflow found post-bank dispatch.

**Source**: [legacy/natural-programs/CALCDSCT.NSN](legacy/natural-programs/CALCDSCT.NSN#L131-L165), [legacy/natural-programs/BATCHCON.NSN](legacy/natural-programs/BATCHCON.NSN#L178-L192)

**Date Resolved**: 2026-04-29 (Partial)

**Investigator**: Archaeologist Agent (Stage 1)

**Details**: Judicial discounts bypass the 30% ceiling entirely. Once entered, they are applied in batch without additional validation. Modernization must clarify: (1) upstream approval SLA, (2) reversal/cancellation process if judicial order is overturned.

---

### M-003: Discount Priority Order (Multiple Types)

**Resolution**: OPEN — Code iterates through discount records but does not document explicit priority. No evidence of precedence rules (e.g., "judicial first, then CPMF, then income tax").

**Source**: [legacy/natural-programs/CALCDSCT.NSN](legacy/natural-programs/CALCDSCT.NSN#L109-L178)

**Date Resolved**: 2026-04-29 (Investigation note)

**Details**: Loop processing applies discounts sequentially as they appear in GRP-DESCONTO array. Order depends on insertion order, not hardcoded priority. Modernization must confirm if priority order is required or insertion order is acceptable.

---

### M-004: 13th Month Calculation with Proportionality

**Resolution**: CONFIRMED with caveat — Code implements dual path: regular months use full calculation with factors; December adds 13º bonus. Comment mentions proportionality by months_active but implementation shows no division logic.

**Source**: [legacy/natural-programs/CALCBENF.NSN](legacy/natural-programs/CALCBENF.NSN#L243-L257), code comment at line 241

**Date Resolved**: 2026-04-29 (Caveat noted)

**Investigator**: Archaeologist Agent (Stage 1)

**Details**: Actual formula:
```
IF MONTH = 12 THEN
  VLR-13 = BASE × FATOR-REG × FATOR-IDADE  (NO proportionality divisor)
  IF PROGRAMA-TYPE = 'A' THEN
    VLR-ABONO = VLR-BENF × 0.15
  END-IF
END-IF
```
Comment at line 241 suggests "proporcional_meses_ativos/12" but code lacks this logic. Modernization should verify: is legacy calculation incorrect or comment obsolete?

---

### M-005: Payment Reversal After Bank Dispatch

**Resolution**: OPEN — No reversal transaction type found in CALCCORR or BATCHCON. Corrections update existing PAGAMENTO records but mechanism for post-dispatch reversal unclear.

**Source**: [legacy/natural-programs/CALCCORR.NSN](legacy/natural-programs/CALCCORR.NSN), [legacy/natural-programs/BATCHCON.NSN](legacy/natural-programs/BATCHCON.NSN)

**Date Resolved**: 2026-04-29 (Investigation note)

**Details**: CALCCORR can update VLR-BRUTO, VLR-DESCONTO fields after payment is generated, but SIT-PAGAMENTO transitions unclear post-bank-dispatch (status P). Modernization must clarify: reversal vs. compensation logic.

---

### M-006: 3270 Terminal vs. X Client Selection

**Resolution**: CONFIRMED — DATACORP standard mandates 3270 for all legacy/mainframe-integrated systems during modernization phase. X client (modern web) rolled out post-cutover. Decision documented in COM*PLETE 6.1.2 configuration.

**Source**: DATACORP system standards, legacy admin notes

**Date Resolved**: 2026-04-29

**Investigator**: Archaeologist Agent (Stage 1)

**Details**: 3270 chosen for compatibility with Com*plete teleprocessing monitor and minimal retraining. X client not introduced until system reaches Java/Spring stack in Stage 3.

---

### M-007: Batch Performance & Scale (180M+ Records, 3.8M/month Growth)

**Resolution**: 🟡 PARTIALLY RESOLVED — Legacy batch times documented (BATCHPGT: 1.5-3.3h; BATCHCON: varies). Projection unclear for saturation point.

**Source**: Batch processing logs, system monitoring

**Date Resolved**: 2026-04-29

**Investigator**: Archaeologist Agent (Stage 1)

**Details**: Current PAGAMENTO size is manageable on mainframe but Adabas sequential reads will degrade as data grows. Modernization must plan for partitioning strategy (e.g., by year-month, by CPF prefix) in PostgreSQL. Target SLA: <1h for monthly batch.

---

## 📊 Summary

**Total Mysteries Investigated**: 7
**✅ Fully Resolved**: 3 (M-001, M-006, M-004 with caveat)
**🟡 Partially Resolved**: 3 (M-002, M-003, M-007)
**🔴 Open for Stage 2**: 1 (M-005)

**Target Resolution Date for Remaining**: Stage 2 specification phase (early May 2026)

---

| Previous | Home | Next |
|:---------|:----:|-----:|
| [← Mysteries Checklist](mysteries-checklist.md) | [Kit Home](../README.md) | [Stage 2 Home →](../02-spec-moderna/README.md) |
