---
title: "Program Dependency Map"
description: "Call graph and data flow for SIFAP legacy Natural programs"
author: "Paula Silva, AI-Native Software Engineer, Americas Global Black Belt at Microsoft"
date: "2026-04-29"
version: "1.2.0"
status: "approved"
tags: ["stage-1", "dependencies", "call-graph", "data-flow"]
---

# 🗺️ Program Dependency Map — SIFAP Legacy

> Visual and textual mapping of program dependencies, call hierarchies, and data flows in legacy SIFAP.

---

## 📑 Table of Contents

1. [📊 Program Call Hierarchy](#program-call-hierarchy)
2. [🔄 Data Flow Diagram](#data-flow-diagram)
3. [📦 Data Entities and DDMs](#data-entities-and-ddms)
4. [📋 Program Matrix: Dependencies](#program-matrix-dependencies)
5. [🔗 External System Integrations](#external-system-integrations)
6. [⏱️ Call Frequency and Performance Notes](#call-frequency-and-performance-notes)
7. [🛤️ Critical Paths for Modernization](#critical-paths-for-modernization)

---

---

## 📊 Program Call Hierarchy

### Entry Point Programs (User-Facing & Batch)

```
┌─ CADBENEF (Register Beneficiary)
│  ├─ VALBENEF (validate: CPF, name, date, UF, status)
│  └─ BENEFICIARIO (Adabas persist)
│
├─ BATCHPGT (Monthly Payment Generation) [Monthly, 1st business day]
│  ├─ BENEFICIARIO (read by CPF)
│  ├─ PROGRAMA-SOCIAL (lookup and parameters)
│  ├─ CALCBENF (calculate base amount with factors)
│  ├─ CALCDSCT (apply discounts, enforce 30% ceiling)
│  ├─ PAGAMENTO (store payment record)
│  └─ AUDITORIA (log batch event)
│
├─ BATCHCON (Bank Reconciliation) [Monthly, after payment dispatch]
│  ├─ PAGAMENTO (read and update status)
│  ├─ AUDITORIA (log reconciliation events)
│  └─ Bank CNAB 240 file (input)
│
├─ BATCHREL (Payment Release to Bank)
│  ├─ PAGAMENTO (read by status=G)
│  └─ CNAB 240 output
│
├─ RELAUDIT (Audit Trail Report) [Manual demand]
│  └─ AUDITORIA (read, filter action codes)
│
└─ RELPGT (Payment Report) [Manual demand]
   └─ PAGAMENTO (read with date range filter)
```

### Supporting Programs (Internal Logic)

```
┌─ CALCBENF (Benefit Calculation)
│  └─ Applies factors: regional, family, income, age + reajuste
│
├─ CALCDSCT (Discount Calculation)
│  └─ Enforces ceiling (30% non-judicial, unlimited judicial)
│
├─ CALCCORR (Correction/Adjustment)
│  └─ Updates existing PAGAMENTO + AUDITORIA
│
├─ VALBENEF (Beneficiary Validation)
│  └─ CPF modulo-11, name, date, UF, status domain
│
├─ CONSBENF (Beneficiary Consultation) [Terminal UI]
│  └─ BENEFICIARIO (read by CPF filter)
│
└─ VALDOCS (Legal Document Validation)
   └─ External validation service call
```
│  │  └─ CALCPAY (calls main calculation)
│  └─ SEND-NOTIFICATIONS (utility)
│
└─ MONTHLY-CLOSING (Month-end)
   └─ GENERATE-JOURNAL (accounting integration)
```

---

## 🔄 Data Flow Diagram

### Beneficiary Registration Flow

```
USER INPUT (CADBENEF)
   |
   v
VALBENEF
   |
   +---> [CPF valid (modulo-11)?] --NO--> REJECT
   |     [Name has space?] --NO--> REJECT
   |     [Birth date valid?] --NO--> REJECT
   |     [UF in 27-state table?] --NO--> REJECT
   |     [Status in A/S/C/I/D?] --NO--> REJECT
   |
   v (all validations pass)
BENEFICIARIO
   |
   v
[Create record with Status = A]
   |
   v
AUDITORIA
   |
   v
[Log: COD-ACAO=IN, timestamp UTC]
   |
   v
ACCEPT
   |
   v
USER OUTPUT
```

### Monthly Payment Generation Flow (BATCHPGT)

```
SCHEDULED: 1st business day of month, 22:00h
   |
   v
BATCHPGT
   |
   +---> For each BENEFICIARIO where STATUS=A
   |
   +---> [Payment exists for ANO-MES-REF?] --YES--> SKIP
   |
   v (Duplicate check passed)
PROGRAMA-SOCIAL
   |
   +---> Retrieve base amount and parameters
   |
   +---> CALCBENF
         |
         +---> base * fator-regional * fator-familia
         +---> * fator-renda * fator-idade * (1 + reajuste)
         |
         +---> [IF MONTH=12: add 13º bonus]
         +---> [IF PROGRAM=A & MONTH=12: add 15% abono]
         |
         v
      Return VLR-BRUTO
         |
         v
   +---> CALCDSCT
         |
         +---> For each TIPO-DESCONTO
         +---> IF TIPO-DESCONTO != 'J' AND total > 30% THEN
         |     TRUNCATE to 30%
         |
         v
      Return VLR-DESCONTO
         |
         v
   +---> Calculate VLR-LIQUIDO = VLR-BRUTO - VLR-DESCONTO
   |
   +---> PAGAMENTO (STORE)
   |     [Status = G (gerado)]
   |
   +---> AUDITORIA (STORE)
   |     [COD-ACAO=IN, TIPO-ENTIDADE=PGTO]
   |
   v
Summary: logged to batch report
   |
   v
COMPLETE
```

### Bank Reconciliation Flow (BATCHCON)

```
INPUT: Bank return file (CNAB 240)
   |
   v
BATCHCON
   |
   +---> For each return record
   |
   +---> Read PAGAMENTO by NUM-PAGTO
   |
   +---> [Payment found?] --NO--> Log divergence
   |     |
   |     YES
   +---> [Amount matches?] --NO--> Log divergence
   |     |
   |     YES
   v
UPDATE PAGAMENTO
   |
   +---> Status from G → P (pagamento)
   |
   v
AUDITORIA (STORE)
   |
   +---> [Log: COD-ACAO=CO (confirmed)]
   |
   v
COMPLETE
```

### Audit Report Flow (RELAUDIT)

```
ENTRY: RELAUDIT (date range)
   |
   v
AUDITORIA (read with filters)
   |
   +---> Filter: date range (DT-EVENTO)
   +---> Exclude: COD-ACAO='EX' (deletions not shown)
   |
   v
GROUP BY COD-ACAO
COUNT by operation type
   |
   v
FORMAT-REPORT
   |
   v
OUTPUT: 66-line mainframe report
```
```

---

## 📦 Data Entities and DDMs

### DDM: BENEFIC (Beneficiary Master)

```
BENEFIC (Adabas DDM)
├─ ID (PIC 9(8), key)
├─ CPF (PIC X(11), unique index)
├─ Name (PIC X(100))
├─ Status (PIC X(20), indexed)
│  └─ Values: ACTIVE, SUSPENDED, CANCELLED
├─ Email (PIC X(100))
├─ Phone (PIC X(20))
├─ BenefitType (PIC X(3))
│  └─ Values: R (Regular), E (Extended), S (Special)
├─ CreatedAt (PIC X(19), formatted YYYY-MM-DD HH:MM:SS)
├─ ModifiedAt (PIC X(19), formatted YYYY-MM-DD HH:MM:SS)
└─ ModifiedBy (PIC X(20), user ID)
```

### DDM: PAYMENT

```
PAYMENT (Adabas DDM)
├─ ID (PIC 9(10), key)
├─ BeneficID (PIC 9(8), foreign key to BENEFIC)
├─ CycleDate (PIC X(7), YYYY-MM format)
├─ BaseAmount (DEC(13,2))
├─ DiscountTotal (DEC(13,2))
├─ NetAmount (DEC(13,2))
├─ Status (PIC X(20), indexed)
│  └─ Values: APPROVED, PAID, REJECTED, CANCELLED
├─ PaymentDate (PIC X(10), YYYY-MM-DD)
├─ CreatedAt (PIC X(19), UTC)
├─ CreatedBy (PIC X(20), user ID)
└─ ModifiedAt (PIC X(19), UTC)
```

### DDM: DISCOUNT

```
DISCOUNT (Adabas DDM)
├─ ID (PIC 9(10), key)
├─ BeneficID (PIC 9(8), foreign key to BENEFIC)
├─ Type (PIC X(3), indexed)
│  └─ Values: J (Judicial), C (CPMF), I (Income Tax), etc.
├─ Amount (DEC(13,2))
├─ EffectiveFrom (PIC X(10), YYYY-MM-DD)
├─ EffectiveTo (PIC X(10), YYYY-MM-DD)
├─ Reason (PIC X(500))
├─ CreatedAt (PIC X(19), UTC)
└─ CreatedBy (PIC X(20))
```

### DDM: AUDIT

```
AUDIT (Adabas DDM)
├─ ID (PIC 9(12), auto-increment)
├─ EntityType (PIC X(20))
│  └─ Values: BENEFICIARY, PAYMENT, DISCOUNT
├─ EntityID (PIC 9(10))
├─ Operation (PIC X(10))
│  └─ Values: CREATE, UPDATE, DELETE
├─ OldValue (PIC X(4000))
├─ NewValue (PIC X(4000))
├─ Timestamp (PIC X(26), UTC ISO 8601)
├─ UserID (PIC X(20))
├─ SourceProgram (PIC X(20))
└─ IPAddress (PIC X(15))
```

---

## 📋 Program Matrix: Dependencies

| Program | Calls | Called By | Purpose |
|---------|-------|-----------|---------|
| REGISTBN | VALIDATE-CPF, CHECK-DUPLICATE, UPDATE-BENEFIC | User | Register new beneficiary |
| VALIDATE-CPF | (none) | REGISTBN | Validate CPF with modulo-11 |
| CHECK-DUPLICATE | (none) | REGISTBN | Check if CPF already exists |
| CALCPAY | GET-BENEFIC, CALCULATE-DISCOUNT, VALIDATE-PAYMENT, PERSIST-PAYMENT, STORE-AUDIT | NIGHTLY-BATCH | Calculate payment amount |
| CALCULATE-DISCOUNT | GET-DISCOUNTS | CALCPAY | Sum and apply discount rules |
| VALIDATE-PAYMENT | (none) | CALCPAY | Validate business rules |
| GENRPT | READ-AUDIT, READ-PAYMENTS, FORMAT-REPORT, EXPORT-PDF | User | Generate report PDF |
| NIGHTLY-BATCH | PROCESS-CYCLE, SEND-NOTIFICATIONS | Scheduler | Nightly job (triggers CALCPAY for each beneficiary) |
| PROCESS-CYCLE | CALCPAY | NIGHTLY-BATCH | Process payment cycle |

---

## 🔗 External System Integrations

```
SIFAP
├─ Adabas (Database)
│  ├─ BENEFIC.DDM
│  ├─ PAYMENT.DDM
│  ├─ DISCOUNT.DDM
│  └─ AUDIT.DDM
│
├─ Central Government Systems
│  ├─ CPF Validation Service (external API, if available)
│  └─ Judicial System (receives garnishment notifications)
│
└─ PDF Export Library
   └─ PDF generation utility
```

---

## ⏱️ Call Frequency and Performance Notes

| Program | Daily Calls | Peak Time | Avg Duration |
|---------|-------------|-----------|--------------|
| REGISTBN | < 50 | Morning | < 1 sec |
| CALCPAY | 10,000+ | Night (nightly batch) | 50 msec (per record) |
| GENRPT | 10-20 | End of month | 2-5 minutes (full report) |
| NIGHTLY-BATCH | 1 | 02:00 AM | 30-45 minutes (entire cycle) |

---

## 🛤️ Critical Paths for Modernization

### Path 1: Beneficiary Management
REGISTBN -> VALIDATE-CPF -> UPDATE-BENEFIC

**Modern equivalent**: BeneficiaryController.register() -> BeneficiaryService.register() -> BeneficiaryRepository.save()

### Path 2: Payment Calculation
NIGHTLY-BATCH -> CALCPAY -> CALCULATE-DISCOUNT -> PERSIST-PAYMENT -> STORE-AUDIT

**Modern equivalent**: PaymentProcessingScheduler -> PaymentService.calculateForCycle() -> DiscountService.calculateTotal() -> PaymentRepository.save() + AuditService.record()

### Path 3: Reporting
GENRPT -> READ-AUDIT/READ-PAYMENTS -> FORMAT-REPORT -> EXPORT-PDF

**Modern equivalent**: ReportController.generate() -> AuditRepository.findByDateRange() -> ReportService.generate() -> PdfExporter.export()

---

| Previous | Home | Next |
|:---------|:----:|-----:|
| [← Business Rules](business-rules-catalog.md) | [Kit Home](../README.md) | [Discovery Report →](discovery-report.md) |
