---
title: "SIFAP 2.0 Specification"
description: "Complete EARS requirements specification for modernized SIFAP system"
author: "Paula Silva, AI-Native Software Engineer, Americas Global Black Belt at Microsoft"
date: "2026-04-29"
version: "1.0.0"
status: "draft"
tags: ["stage-2", "specification", "requirements", "ears", "sifap-2.0"]
---

# SIFAP 2.0 — Requirements Specification

> **Status**: Stage 2 In Progress  
> **Last Updated**: 2026-04-29  
> **Requirements Count**: 15 (from Stage 1 business rules) + 19 (modern capabilities) = **34 EARS Requirements**

---

## 📋 Table of Contents

1. [Executive Summary](#-executive-summary)
2. [Beneficiary Management Requirements (REQ-BEN-*)](#-beneficiary-management-requirements-req-ben-)
3. [Payment Processing Requirements (REQ-PAY-*)](#-payment-processing-requirements-req-pay-)
4. [Benefit Calculation Requirements (REQ-CALC-*)](#-benefit-calculation-requirements-req-calc-)
5. [Audit & Retention Requirements (REQ-AUD-*, REQ-DATA-*)](#-audit--retention-requirements-req-aud--req-data-)
6. [Modern Capabilities (REQ-UI-*, REQ-API-*, REQ-SEC-*, REQ-PERF-*)](#-modern-capabilities-placeholder)
7. [Non-Functional Requirements](#-non-functional-requirements)
8. [Traceability Matrix](#-traceability-matrix)
9. [Glossary](#-glossary)

---

## 📊 Executive Summary

SIFAP 2.0 is a modernized social benefits payment system migrating from Natural/Adabas (mainframe) to Java 21 + Spring Boot 3.3 + PostgreSQL 16 + Next.js 15.

**Scope**: Full functional migration of legacy SIFAP (16 Natural programs, 4 DDMs, 14 business rules) plus new web UI, REST APIs, OAuth2 authentication, and performance optimizations.

**Data Volume**: 4.2M beneficiaries, 180M+ payment records (28-year history, no purge), 25M audit records (immutable).

**Target SLA**: Payment batch <1 hour (vs. legacy 1.5-3.3h), API response <500ms (p99).

---

## 👤 Beneficiary Management Requirements (REQ-BEN-*)

### REQ-BEN-001: Validate Beneficiary CPF

**Type**: Ubiquitous

**Statement**: The system shall validate beneficiary CPF using the modulo-11 algorithm as defined by the Brazilian Tax Authority (Receita Federal).

**Source Code**: [VALBENEF.NSN lines 113-236](../legacy/natural-programs/VALBENEF.NSN#L113)

**Acceptance Criteria**:
1. **Given** a valid CPF "123.456.789-10", **when** registering a beneficiary, **then** the system accepts the registration
2. **Given** an invalid CPF "123.456.789-11" (wrong check digit), **when** registering a beneficiary, **then** the system rejects with error "Invalid CPF: failed modulo-11 validation"
3. **Given** an all-zeros CPF "000.000.000-00", **when** registering a beneficiary, **then** the system rejects as reserved invalid code

**Traceability**: BR-BEN-001 (Stage 1 business rule catalog)

**Implementation Notes**:
- CPF format: 11 digits (can be formatted as XXX.XXX.XXX-XX for display)
- Algorithm: Modulo-11 with weights 10-2 (1st digit), 11-2 (2nd digit)
- Test vectors: Valid 111.444.777-35, Invalid 111.444.777-36

---

### REQ-BEN-002: Validate Beneficiary Name

**Type**: Ubiquitous

**Statement**: The system shall require beneficiary full name to contain at least a given name and surname separated by space.

**Source Code**: [VALBENEF.NSN lines 264-270](../legacy/natural-programs/VALBENEF.NSN#L264)

**Acceptance Criteria**:
1. **Given** a name "João Silva", **when** registering a beneficiary, **then** the system accepts it
2. **Given** a single-word name "João", **when** registering a beneficiary, **then** the system rejects with error "Name must contain surname (space-separated)"
3. **Given** an empty or blank name, **when** registering a beneficiary, **then** the system rejects with error "Name is required"

**Traceability**: BR-BEN-002

---

### REQ-BEN-003: Validate Birth Date

**Type**: Ubiquitous

**Statement**: The system shall validate beneficiary birth date to be within the range 1900-01-01 to current date, with valid month (1-12) and day for the given month.

**Source Code**: [VALBENEF.NSN lines 248-256](../legacy/natural-programs/VALBENEF.NSN#L248)

**Acceptance Criteria**:
1. **Given** a birth date "1980-06-15" (valid), **when** registering a beneficiary, **then** the system accepts it
2. **Given** a birth date "1800-06-15" (before 1900), **when** registering a beneficiary, **then** the system rejects with error "Birth year must be 1900 or later"
3. **Given** a birth date "1985-13-01" (invalid month), **when** registering a beneficiary, **then** the system rejects with error "Invalid month"
4. **Given** a birth date "1985-02-30" (invalid day for February), **when** registering a beneficiary, **then** the system rejects with error "Invalid day for February"

**Traceability**: BR-BEN-003

---

### REQ-BEN-004: Validate State (UF)

**Type**: Ubiquitous

**Statement**: The system shall validate beneficiary state (UF) against the official list of 27 Brazilian states and Federal District.

**Source Code**: [VALBENEF.NSN lines 145-148](../legacy/natural-programs/VALBENEF.NSN#L145)

**Acceptance Criteria**:
1. **Given** a state "SP" (São Paulo), **when** registering a beneficiary, **then** the system accepts it
2. **Given** a state "XY" (invalid code), **when** registering a beneficiary, **then** the system rejects with error "Invalid state code. Valid values: AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA, PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO"
3. **Given** the code "df" (lowercase), **when** registering a beneficiary, **then** the system accepts it (case-insensitive normalization to "DF")

**Traceability**: BR-BEN-004

---

### REQ-BEN-005: Validate Status Domain

**Type**: Ubiquitous

**Statement**: The system shall accept only valid beneficiary status codes: A (active), S (suspended), C (cancelled), I (inactive), D (deleted).

**Source Code**: [VALBENEF.NSN line 164](../legacy/natural-programs/VALBENEF.NSN#L164)

**Acceptance Criteria**:
1. **Given** a status "A", **when** registering a beneficiary, **then** the system accepts it
2. **Given** a status "X" (invalid), **when** registering a beneficiary, **then** the system rejects with error "Invalid status. Valid values: A, S, C, I, D"
3. **Given** an empty status, **when** registering a beneficiary, **then** the system defaults to "A" (active)

**Traceability**: BR-BEN-005

---

## 💳 Payment Processing Requirements (REQ-PAY-*)

### REQ-PAY-001: Prevent Duplicate Payments by Competence

**Type**: Event-driven

**Statement**: When a payment generation cycle is triggered for a beneficiary and competence (AAAAMM), the system shall check if a payment already exists for that combination and skip generation if one exists.

**Source Code**: [BATCHPGT.NSN lines 202-335](../legacy/natural-programs/BATCHPGT.NSN#L202)

**Acceptance Criteria**:
1. **Given** a beneficiary "123.456.789-10" with no existing payment for "202604", **when** BATCHPGT runs, **then** a new payment record is created
2. **Given** a beneficiary "123.456.789-10" with existing payment for "202604", **when** BATCHPGT runs again, **then** the existing payment is not duplicated (skip logic)
3. **Given** a beneficiary "123.456.789-10" with payment for "202603" (previous month), **when** BATCHPGT runs for "202604", **then** a new payment is created for "202604"

**Traceability**: BR-PAY-001

**Implementation Notes**:
- Competence is AAAAMM format (year + month, e.g., 202604 for April 2026)
- Descriptor on PAGAMENTO table: (CPF-BENEF, ANO-MES-REF, COD-PROGRAMA)
- Avoid using unique constraint; use SELECT before INSERT pattern for idempotency

---

### REQ-PAY-002: Payment Status Domain

**Type**: Ubiquitous

**Statement**: The system shall enforce a strict domain of payment status codes and reject any status value outside this set.

**Source Code**: [PAGAMENTO.ddm line 48](../legacy/adabas-ddms/PAGAMENTO.ddm#L48)

**Status Domain**:
- **P**: Pending (awaiting dispatch)
- **G**: Generated (ready for dispatch)
- **E**: Error (processing failed)
- **C**: Confirmed (bank confirmed receipt)
- **D**: Devolved (bank returned unpaid)
- **X**: Canceled (manually canceled)
- **R**: Reversed (post-payment reversal)

**Acceptance Criteria**:
1. **Given** a payment with status "G", **when** checking valid values, **then** the system accepts it
2. **Given** a payment with status "Z", **when** creating/updating, **then** the system rejects with error "Invalid status 'Z'. Valid values: P, G, E, C, D, X, R"
3. **Given** a payment status transition G → P (generated to pending), **when** confirming dispatch, **then** the system allows it

**Traceability**: BR-PAY-002

---

### REQ-PAY-003: Non-Judicial Discount Ceiling (30%)

**Type**: Optional

**Statement**: Where a discount type is NOT judicial (J), the system shall limit the total discount amount to a maximum of 30% of the base payment amount. Judicial discounts are exempt from this ceiling.

**Source Code**: [CALCDSCT.NSN lines 101-165](../legacy/natural-programs/CALCDSCT.NSN#L101)

**Acceptance Criteria**:
1. **Given** a payment with base amount 1000 and non-judicial discounts totaling 350 (35%), **when** calculating discounts, **then** the system truncates to 300 (30%)
2. **Given** a payment with base amount 1000 and judicial discount 600, **when** calculating discounts, **then** the system accepts the full 600 (no ceiling)
3. **Given** a payment with base amount 1000 and discounts [judicial 200 + CPMF 400], **when** calculating, **then** judicial 200 is applied fully, CPMF truncated to 300 (30% of base), total discount = 500

**Traceability**: BR-PAY-003

**Discount Types**:
- J = Judicial (court order)
- C = CPMF (Contribuição Provisória sobre Movimentação Financeira)
- I = Income Tax (Imposto de Renda)
- O = Other (customizable)

---

### REQ-PAY-004: Corrections Update Existing Records

**Type**: Event-driven

**Statement**: When a payment correction is requested (e.g., due to data error), the system shall update the existing payment record and create an audit trail entry documenting the old and new values.

**Source Code**: [CALCCORR.NSN lines 128-162](../legacy/natural-programs/CALCCORR.NSN#L128)

**Acceptance Criteria**:
1. **Given** an existing payment record with VLR-BRUTO=1000, **when** a correction updates it to 1050, **then** the payment record is updated and an audit entry is created with action=AL (alter)
2. **Given** a correction that changes multiple fields (VLR-BRUTO and VLR-DESCONTO), **when** applied, **then** the audit entry records before/after values for all changed fields
3. **Given** a correction 30 days after payment creation, **when** applied, **then** DT-ATUALIZACAO is updated and audit trail shows who/when/what changed

**Traceability**: BR-PAY-004

---

## 🧮 Benefit Calculation Requirements (REQ-CALC-*)

### REQ-CALC-001: Regular Monthly Benefit Calculation

**Type**: Ubiquitous

**Statement**: The system shall calculate monthly benefit amount using the formula: Base × Factor-Regional × Factor-Family × Factor-Income × Factor-Age × (1 + Reajuste), then round to 2 decimal places.

**Source Code**: [CALCBENF.NSN lines 225-229](../legacy/natural-programs/CALCBENF.NSN#L225)

**Formula**: 
```
VLR-BENEF = BASE × FR × FF × FI × FA × (1 + Reajuste)
Round(VLR-BENEF, 2)
```

**Acceptance Criteria**:
1. **Given** a beneficiary with base=1000, FR=1.1, FF=1.0, FI=1.0, FA=1.0, reajuste=0.05, **when** calculating benefit, **then** result = 1155.00
2. **Given** a beneficiary with decimal factors resulting in amount=1234.567, **when** calculating, **then** result is rounded to 1234.57 (2 decimal places)
3. **Given** a beneficiary in December (month=12), **when** calculating regular benefit, **then** calculation is separate from 13th month logic (see REQ-CALC-002)

**Traceability**: BR-CALC-001

**Factors Table**:
| Factor | Description | Range | Source |
|--------|---|---|---|
| FR | Regional adjustment | 0.8-1.3 | PROGRAMA-SOCIAL table |
| FF | Family size | 1.0-1.5 | Beneficiary dependents count |
| FI | Income adjustment | 0.5-1.0 | Beneficiary reported income |
| FA | Age adjustment | 0.8-1.2 | Beneficiary age at payment |

---

### REQ-CALC-002: December 13th Month Bonus

**Type**: State-driven

**Statement**: While processing a payment in December (month=12), the system shall add a 13th month bonus equal to Base × Factor-Regional × Factor-Age, calculated separately and added to the regular monthly benefit.

**Source Code**: [CALCBENF.NSN lines 243-249](../legacy/natural-programs/CALCBENF.NSN#L243)

**Acceptance Criteria**:
1. **Given** a payment processed in December with regular benefit=1000, **when** calculating, **then** a 13th month bonus is calculated as BASE × FR × FA = 1000 × 1.1 × 1.0 = 1100, total = 2100
2. **Given** a payment processed in January-November, **when** calculating, **then** no 13th month bonus is added
3. **Given** a payment in December with base=2000, FR=1.0, FA=0.9, **when** calculating 13º, **then** bonus = 1800, total = 3800 (2000 + 1800)

**Traceability**: BR-CALC-002

**Formula**:
```
IF MONTH(competencia) = 12 THEN
  VLR-13 = BASE × FR × FA
  VLR-TOTAL = VLR-BENEF + VLR-13
END-IF
```

---

### REQ-CALC-003: Program Type A Abono (15% December Bonus)

**Type**: Optional

**Statement**: Where the beneficiary's program type is "A" and the payment cycle is December, the system shall add an additional 15% abono (bonus) of the regular monthly benefit amount.

**Source Code**: [CALCBENF.NSN lines 252-257](../legacy/natural-programs/CALCBENF.NSN#L252)

**Acceptance Criteria**:
1. **Given** a beneficiary with program type "A" in December with regular benefit=1000, **when** calculating, **then** abono = 1000 × 0.15 = 150, added to total (after 13º)
2. **Given** a beneficiary with program type "B" in December, **when** calculating, **then** no abono is added
3. **Given** a beneficiary with program type "A" in January, **when** calculating, **then** no abono is added (December only)

**Traceability**: BR-CALC-003

---

## 🔒 Audit & Retention Requirements (REQ-AUD-*, REQ-DATA-*)

### REQ-AUD-001: Immutable Audit Trail with 10-Year Retention

**Type**: Ubiquitous

**Statement**: The system shall maintain a permanent, immutable audit trail recording all changes to beneficiary and payment data, with a minimum retention period of 10 years (per Lei 8159 - Brazilian Law on Archives).

**Source Code**: [AUDITORIA.ddm lines 13-91](../legacy/adabas-ddms/AUDITORIA.ddm#L13)

**Audit Attributes**:
- **NUM-AUDITORIA**: Unique sequential ID
- **DT-EVENTO**: Event date (YYYYMMDD)
- **HR-EVENTO**: Event time (HHMMSS)
- **TS-EVENTO**: Event timestamp (YYYYMMDDHHMMSS.mmm)
- **COD-ACAO**: Action code (IN=insert, AL=alter, EX=delete attempt, CO=confirm, DV=divergence)
- **TIPO-ENTIDADE**: Entity type (BENF=beneficiary, PGTO=payment, PROG=program)
- **ID-ENTIDADE**: Entity primary key
- **GRP-ANTES**: Old field values (for UPDATE)
- **GRP-DEPOIS**: New field values (for UPDATE)
- **USR-EVENTO**: User ID who triggered action
- **COD-MODULO**: Source program name (CADBENEF, BATCHPGT, etc.)

**Acceptance Criteria**:
1. **Given** a new beneficiary registration by user "OPJOSE", **when** the system stores it, **then** an audit record is created with COD-ACAO=IN, TIPO-ENTIDADE=BENF, TS-EVENTO=current timestamp, USR-EVENTO=OPJOSE
2. **Given** a beneficiary status update from "A" to "S", **when** the change is applied, **then** the audit record includes GRP-ANTES (old values) and GRP-DEPOIS (new values)
3. **Given** an audit record created in 2016, **when** retrieving in 2026, **then** the record is still present (10-year retention met); **when** requesting deletion, **then** the system rejects with "Audit records are immutable and cannot be deleted"
4. **Given** a user attempting to directly UPDATE or DELETE from the audit table, **when** executing SQL, **then** database constraint/trigger prevents the operation

**Traceability**: BR-AUD-001

---

### REQ-AUD-002: Audit Report Filters Deletion Actions

**Type**: Optional

**Statement**: Where generating a standard audit report, the system shall exclude action code "EX" (delete attempts) from the default view to avoid confusion, while maintaining full audit trail in database.

**Source Code**: [RELAUDIT.NSN line 108](../legacy/natural-programs/RELAUDIT.NSN#L108)

**Acceptance Criteria**:
1. **Given** a request for "standard audit report" for a date range, **when** executing the report, **then** all action codes are shown (IN, AL, CO, DV, etc.) except "EX"
2. **Given** a request for "full audit report" (with advanced filter), **when** including action "EX", **then** deletion attempts are displayed
3. **Given** an audit trail with 100 records (95 normal, 5 EX), **when** generating standard report, **then** report shows 95 records

**Traceability**: BR-AUD-002

---

### REQ-DATA-001: Payment Records Retention (No Purge Policy)

**Type**: Ubiquitous

**Statement**: The system shall retain all payment records permanently without purge, as required by Brazilian compliance regulations and operational continuity. Payment data since 1998 shall be migrated and preserved in the modernized system.

**Source Code**: [PAGAMENTO.ddm lines 105-106](../legacy/adabas-ddms/PAGAMENTO.ddm#L105)

**Data Volume**:
- **Beneficiaries**: ~4.2M active records
- **Payments**: 180M+ records (28-year history, no purge)
- **Monthly Growth**: ~3.8M new payment records
- **Audit Records**: 25M+ immutable entries

**Acceptance Criteria**:
1. **Given** the legacy PAGAMENTO table with 180M+ records (1998-2026), **when** migrating to modern system, **then** all records are transferred and queryable
2. **Given** a payment record from 1998, **when** querying by competence filter, **then** the record is found and displayed with original values
3. **Given** a modern system storage design, **when** planning for growth, **then** the schema supports partitioning by year-month or other strategy to maintain <1h batch SLA with 180M+ records

**Traceability**: BR-DATA-001

---

## 🌐 Modern Capabilities

### REQ-UI-* : User Interface Requirements

#### REQ-UI-001: Responsive Web Interface

**Type**: Ubiquitous

**Statement**: The system shall provide a modern web-based user interface using Next.js 15 and Tailwind CSS that is responsive and functional across desktop, tablet, and mobile devices with screen sizes from 320px to 2560px.

**Technology Stack**: Next.js 15 (App Router), React 19, TypeScript, Tailwind CSS, shadcn/ui

**Acceptance Criteria**:
1. **Given** a desktop browser (1920x1080), **when** accessing the beneficiary registration form, **then** all fields are visible and properly aligned
2. **Given** a tablet (768x1024), **when** accessing the same form, **then** the layout adapts to single column with no horizontal scroll
3. **Given** a mobile device (375x667), **when** accessing the form, **then** all interactive elements are touchable (min 44x44px) and readable without zoom

**Acceptance Test**: BDD test using Playwright testing all viewport sizes

---

#### REQ-UI-002: Dark Mode Theme Support

**Type**: Optional

**Statement**: Where a user prefers dark mode, the system shall provide a dark color scheme throughout the UI with reduced eye strain (contrast ratio minimum 4.5:1 for WCAG AA compliance).

**Implementation**: Tailwind CSS dark: prefix + shadcn/ui dark mode variant

**Acceptance Criteria**:
1. **Given** a user with dark mode preference in OS, **when** accessing the system, **then** dark theme is automatically applied
2. **Given** a user on dark mode, **when** opening the payment list view, **then** all text has contrast ratio ≥4.5:1
3. **Given** a user, **when** toggling theme via UI button, **then** preference is persisted in localStorage and restored on next visit

---

#### REQ-UI-003: Accessibility (WCAG 2.1 Level AA)

**Type**: Ubiquitous

**Statement**: The system shall be accessible to users with disabilities, meeting WCAG 2.1 Level AA standards including keyboard navigation, screen reader support, color contrast, and semantic HTML.

**Acceptance Criteria**:
1. **Given** a user using only keyboard (no mouse), **when** navigating the beneficiary form, **then** all fields and buttons are reachable via Tab key with visible focus indicators
2. **Given** a screen reader user (NVDA/JAWS), **when** accessing the payment list, **then** all data is announced correctly with proper table headers and labels
3. **Given** a page with images, **when** inspecting alt text, **then** all images have descriptive alt attributes (except decorative images with empty alt)
4. **Automated Scan**: axe DevTools scan shall report 0 WCAG violations (errors level)

---

#### REQ-UI-004: Search and Filter Beneficiaries

**Type**: Event-driven

**Statement**: When a user enters search criteria (CPF, name, or status), the system shall filter the beneficiary list in real-time with debouncing (300ms) to avoid excessive API calls.

**Acceptance Criteria**:
1. **Given** a user types "João" in the name filter, **when** 300ms passes, **then** the list is updated to show only beneficiaries matching "João"
2. **Given** a user types "123.456.789" in the CPF field, **when** the field loses focus, **then** the list filters to the exact beneficiary
3. **Given** multiple filter fields (name + status), **when** both are filled, **then** the list shows beneficiaries matching ALL criteria (AND logic)

---

#### REQ-UI-005: Payment History View with Pagination

**Type**: Ubiquitous

**Statement**: The system shall display payment history for a selected beneficiary with pagination (20 records per page), sortable by competence (month), amount, or status.

**Acceptance Criteria**:
1. **Given** a beneficiary with 150 payment records, **when** viewing payment history, **then** the first page shows 20 most recent payments
2. **Given** a user on page 2, **when** clicking "Previous", **then** page 1 is displayed
3. **Given** a user clicking the "Amount" column header, **when** ascending sort is applied, **then** payments are re-ordered by amount (low to high)

---

### REQ-API-* : REST API Requirements

#### REQ-API-001: OpenAPI 3.0 Documentation

**Type**: Ubiquitous

**Statement**: The system shall expose all REST API endpoints with complete OpenAPI 3.0 specification documentation, auto-generated from Spring Boot annotations and accessible via `/api/v1/openapi.json` and Swagger UI at `/api/v1/swagger-ui.html`.

**Technology**: Spring Boot 3.3 + springdoc-openapi

**Acceptance Criteria**:
1. **Given** a developer accessing `/api/v1/swagger-ui.html`, **when** the page loads, **then** all endpoints (GET, POST, PUT, DELETE) are listed with descriptions and example requests/responses
2. **Given** an endpoint `POST /api/v1/beneficiaries`, **when** inspecting the specification, **then** request body schema shows all required fields (CPF, name, birthDate, uf, status)
3. **Given** a response schema, **when** examining error responses, **then** HTTP 400 (validation error), 401 (unauthorized), 404 (not found), 500 (server error) are documented with example payloads

---

#### REQ-API-002: Request Validation and Error Responses

**Type**: Ubiquitous

**Statement**: The system shall validate all API request inputs and return consistent error responses with HTTP status codes, error messages, and field-level validation details.

**Error Response Format**:
```json
{
  "error": {
    "timestamp": "2026-04-29T14:30:00Z",
    "status": 400,
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "details": [
      {
        "field": "cpf",
        "message": "Invalid CPF: failed modulo-11 validation"
      }
    ]
  }
}
```

**Acceptance Criteria**:
1. **Given** a POST `/api/v1/beneficiaries` with invalid CPF "000.000.000-00", **when** the request is submitted, **then** the system returns HTTP 400 with error code "VALIDATION_ERROR" and field detail showing "Invalid CPF"
2. **Given** a POST with missing required field "name", **when** submitted, **then** HTTP 400 response lists the missing field
3. **Given** a GET `/api/v1/beneficiaries/999` where ID does not exist, **when** submitted, **then** HTTP 404 with error code "BENEFICIARY_NOT_FOUND"

---

#### REQ-API-003: Pagination for Large Result Sets

**Type**: Ubiquitous

**Statement**: The system shall support pagination for list endpoints (beneficiaries, payments) using query parameters `page` (1-indexed) and `size` (default 20, max 100), returning metadata about total count and current page.

**Response Format**:
```json
{
  "content": [...],
  "pageNumber": 1,
  "pageSize": 20,
  "totalElements": 1500,
  "totalPages": 75,
  "isFirst": true,
  "isLast": false
}
```

**Acceptance Criteria**:
1. **Given** a request `GET /api/v1/beneficiaries?page=1&size=20`, **when** executed, **then** response contains first 20 beneficiaries with pageNumber=1, totalElements=4200000
2. **Given** a request with `size=150`, **when** submitted, **then** system caps it to 100 (max)
3. **Given** a request `page=999` (beyond total pages), **when** submitted, **then** empty content array is returned with totalPages metadata

---

#### REQ-API-004: Filtering and Search via Query Parameters

**Type**: Ubiquitous

**Statement**: The system shall support filtering and searching via query parameters: `cpf`, `name`, `status`, `createdFrom`, `createdTo` with case-insensitive matching where applicable.

**Acceptance Criteria**:
1. **Given** a request `GET /api/v1/beneficiaries?cpf=123.456.789-10`, **when** executed, **then** response contains the exact beneficiary matching that CPF
2. **Given** a request `GET /api/v1/beneficiaries?name=João`, **when** executed, **then** response contains all beneficiaries with "João" anywhere in name (case-insensitive)
3. **Given** a request `GET /api/v1/beneficiaries?status=A&createdFrom=2026-01-01&createdTo=2026-04-29`, **when** executed, **then** response filters by status AND date range

---

### REQ-SEC-* : Security Requirements

#### REQ-SEC-001: OAuth2 Authentication with Entra ID (Azure AD)

**Type**: Ubiquitous

**Statement**: The system shall authenticate all users via OAuth2 with Azure Entra ID (formerly Azure AD), requiring valid credentials and enforcing multi-factor authentication for privileged roles (admin, auditor).

**Technology**: Spring Security 6.3 + OAuth2 Client + Azure Entra ID

**Acceptance Criteria**:
1. **Given** an unauthenticated user accessing the system, **when** attempting to view beneficiaries, **then** the browser redirects to Azure Entra ID login page
2. **Given** a user logging in with valid Entra ID credentials, **when** login succeeds, **then** the system issues a JWT token and stores it in secure httpOnly cookie
3. **Given** a user in "Admin" Entra ID group, **when** performing privileged action (delete beneficiary), **then** the system requires MFA verification
4. **Given** a user with invalid token, **when** making API request with expired token, **then** HTTP 401 "Unauthorized" is returned

---

#### REQ-SEC-002: Encryption at Rest (Azure Key Vault)

**Type**: Ubiquitous

**Statement**: The system shall encrypt all sensitive data at rest using Azure Key Vault for key management, with database encryption enabled (PostgreSQL pgcrypto or transparent encryption).

**Sensitive Fields**: CPF, beneficiary name, bank account info, discount details

**Acceptance Criteria**:
1. **Given** a beneficiary record stored in database, **when** inspecting the raw data, **then** the CPF field is encrypted (ciphertext visible, not plaintext)
2. **Given** the encryption key stored in Azure Key Vault, **when** rotating the key, **then** the system decrypts old data with old key and re-encrypts with new key without downtime
3. **Given** database backup files, **when** examined offline, **then** sensitive data is not readable (encrypted at rest)

---

#### REQ-SEC-003: Audit Logging of All User Actions

**Type**: Ubiquitous

**Statement**: The system shall log all user actions (read, create, update, delete) with user ID, timestamp, action type, and data affected, maintaining an immutable audit trail separate from the application audit trail (AUDITORIA DDM).

**Audit Trail Storage**: Separate PostgreSQL table or Azure Log Analytics

**Acceptance Criteria**:
1. **Given** a user updating a beneficiary status, **when** the change is saved, **then** the system creates a security audit log entry with username, timestamp, "UPDATE BENEFICIARY status from A to S", and beneficiary CPF (PII masked as XXX.XXX.XXX-10)
2. **Given** a user attempting unauthorized access (e.g., accessing another user's data), **when** the attempt is blocked, **then** a security audit log is created with "UNAUTHORIZED_ACCESS_ATTEMPT" and details
3. **Given** 1 year of audit logs, **when** querying for "all changes by user ADMJOSE", **then** the system returns filtered results with timestamps

---

#### REQ-SEC-004: HTTPS and TLS 1.2+ Encryption in Transit

**Type**: Ubiquitous

**Statement**: The system shall enforce HTTPS for all communication with TLS 1.2 or higher, disable outdated SSL versions, and use certificates issued by trusted Certificate Authority.

**Acceptance Criteria**:
1. **Given** a user accessing the system via HTTP (not HTTPS), **when** the request is made, **then** the server redirects to HTTPS (301 redirect)
2. **Given** a client attempting TLS 1.1 connection, **when** the handshake is attempted, **then** the server rejects it with "TLS version not supported"
3. **Given** a certificate inspection, **when** checking certificate validity, **then** it is issued by a trusted CA (not self-signed) and not expired

---

#### REQ-SEC-005: CORS Policy for API Access

**Type**: Ubiquitous

**Statement**: The system shall restrict Cross-Origin Resource Sharing (CORS) to explicitly allowed origins only, rejecting requests from unauthorized origins.

**Allowed Origins**: https://sifap.datacorp.gov.br, https://sifap-admin.datacorp.gov.br (configurable)

**Acceptance Criteria**:
1. **Given** a frontend on `https://sifap.datacorp.gov.br` making API request to `/api/v1/beneficiaries`, **when** the request includes proper CORS headers, **then** the API allows the request
2. **Given** a malicious site `https://attacker.com` making API request, **when** the request is sent, **then** the browser blocks it due to CORS policy
3. **Given** a preflight OPTIONS request, **when** sent to the API, **then** the system returns allowed methods, allowed headers, and max age

---

### REQ-PERF-* : Performance Requirements

#### REQ-PERF-001: Payment Batch Processing <1 Hour SLA

**Type**: Event-driven

**Statement**: When the monthly payment generation batch (BATCHPGT) is triggered on the 1st business day at 22:00h, the system shall complete payment calculation and creation for all eligible beneficiaries (4.2M+) within 1 hour (vs. legacy 1.5-3.3 hours).

**Optimization Strategies**:
- Parallel processing (Spring Batch partitions)
- Database query optimization (indexes on CPF, competence)
- Caching of program master data (PROGRAMA-SOCIAL)

**Acceptance Criteria**:
1. **Given** 4.2M active beneficiaries, **when** BATCHPGT runs, **then** all payment records are created within 60 minutes
2. **Given** monthly growth of 3.8M new records, **when** BATCHCON (reconciliation) runs, **then** completion time is <30 minutes
3. **Given** peak load test with 5M beneficiaries, **when** batch runs, **then** system completes within 75 minutes with CPU <80%, memory <85%

---

#### REQ-PERF-002: API Response Time <500ms (p99)

**Type**: Ubiquitous

**Statement**: The system shall respond to all synchronous API requests with response time less than 500ms for the 99th percentile (p99), measured from request received to response sent.

**Scope**: GET/POST beneficiary, payment list, report generation (paginated)

**Excluded**: Long-running operations (batch export, report generation >5MB)

**Acceptance Criteria**:
1. **Given** a GET `/api/v1/beneficiaries?cpf=123.456.789-10` (single beneficiary lookup), **when** 100 concurrent requests are made, **then** p99 response time is <100ms
2. **Given** a GET `/api/v1/beneficiaries?page=1&size=20` (list with pagination), **when** 100 concurrent requests are made, **then** p99 response time is <300ms
3. **Given** a GET `/api/v1/payments?cpf=123.456.789-10&page=1` (payment history, 20 records), **when** 100 concurrent requests are made, **then** p99 response time is <400ms
4. **Load Test Tool**: k6 or JMeter with 100 VUs, duration 10 minutes, assertion p99 < 500ms

---

#### REQ-PERF-003: Database Query Optimization

**Type**: Ubiquitous

**Statement**: The system shall maintain efficient database queries with query execution time <100ms for all common operations, using appropriate indexes, query optimization, and avoiding N+1 query problems.

**Index Strategy**:
- BENEFICIARIO: PK (CPF), index on (CPF, STATUS)
- PAGAMENTO: PK (NUM-PAGAMENTO), index on (CPF-BENEF, ANO-MES-REF, STATUS)
- AUDITORIA: PK (NUM-AUDITORIA), index on (DT-EVENTO, COD-ACAO, TIPO-ENTIDADE)

**Acceptance Criteria**:
1. **Given** a query to find payments for CPF "123.456.789-10", **when** the query is executed, **then** execution time is <50ms (with index on CPF-BENEF)
2. **Given** a join query between BENEFICIARIO and PAGAMENTO, **when** joining on CPF, **then** execution time is <150ms for 100 beneficiaries
3. **Query Plan Analysis**: EXPLAIN ANALYZE shall show index usage (no sequential scans on large tables)

---

#### REQ-PERF-004: Caching Strategy for Static Data

**Type**: Ubiquitous

**Statement**: The system shall cache infrequently changing data (PROGRAMA-SOCIAL, UF list, discount types) in memory (Redis or Spring Cache) with TTL of 24 hours to reduce database load.

**Cache Invalidation**: Manual invalidation on admin updates + automatic 24h TTL

**Acceptance Criteria**:
1. **Given** the PROGRAMA-SOCIAL table cached in Redis, **when** 1000 concurrent requests fetch program data, **then** all requests are served from cache (<5ms latency)
2. **Given** an admin updating program base amount, **when** the update is saved, **then** cache is invalidated and subsequent requests fetch fresh data
3. **Given** 24 hours of cache, **when** the TTL expires, **then** cache is automatically refreshed on next access

---

#### REQ-PERF-005: Database Connection Pooling

**Type**: Ubiquitous

**Statement**: The system shall use connection pooling (HikariCP) to manage database connections efficiently with min pool size 10, max pool size 50, and idle timeout 10 minutes.

**Acceptance Criteria**:
1. **Given** the system at normal load, **when** monitoring connection pool, **then** average active connections are 15-25 (not all 50)
2. **Given** sudden spike to 100 concurrent API requests, **when** monitored, **then** system scales to 50 active connections and remaining requests are queued with <5s wait time
3. **Given** idle connections after 10 minutes, **when** checked, **then** they are closed automatically and new requests create fresh connections

---

## ⚙️ Non-Functional Requirements

### Scalability

- **Beneficiaries**: System shall support 10M+ beneficiaries (current: 4.2M) without performance degradation
- **Payment Records**: System shall accommodate 500M+ payment records (current: 180M+) with <1h batch SLA
- **Monthly Growth**: ~3.8M new payment records per month shall be processable within existing SLA
- **Concurrent Users**: System shall support 500+ concurrent API users without timeout
- **Database Partitioning**: PAGAMENTO table shall be partitioned by year-month to maintain query performance across 500M+ records

### Availability & Reliability

- **Uptime SLA**: 99.5% availability (excluding planned maintenance windows)
- **Batch Processing**: BATCHPGT and BATCHCON must complete within defined SLA windows (1h and 30m respectively)
- **Database Backup**: Daily automated backups with 7-day retention (moving to 30-day for production)
- **Disaster Recovery**: RTO (Recovery Time Objective) <4 hours, RPO (Recovery Point Objective) <1 hour
- **Graceful Degradation**: API remains operational even if batch processing is delayed; payments queue and retry automatically

### Compatibility & Migration

- **Non-Destructive Migration**: All legacy data (180M+ payment records, 25M+ audit records) migrated without loss
- **Data Integrity**: Reconciliation reports verify payment totals match exactly between legacy and modern system
- **Parallel Run**: Legacy SIFAP and modern SIFAP run side-by-side for 1 month validation period
- **Cutover Strategy**: Phased rollout by beneficiary subset (5% → 25% → 100%) with rollback capability at each phase
- **Legacy System Retention**: Legacy system remains operational for 90 days post-cutover for emergency fallback

### Compliance & Legal

- **Lei 8159 (Archives Law)**: 10-year minimum retention of audit trail (AUDITORIA records)
- **LGPD (Brazilian GDPR)**: Data privacy, consent management, right to deletion (subject to retention requirements)
- **Immutable Audit Trail**: No deletion or modification of audit records; database constraints enforce this
- **SOX/Financial Audits**: All payment transactions traceable with user/timestamp/amount for external audit
- **PCI DSS (if handling payments)**: If storing bank account details, comply with PCI DSS 3.2.1 (encryption, access controls)

### Technology Stack Commitments

- **Backend**: Java 21 + Spring Boot 3.3 (LTS)
- **Frontend**: Next.js 15 (App Router) + React 19 + TypeScript + Tailwind CSS + shadcn/ui
- **Database**: PostgreSQL 16 with pgcrypto for encryption
- **Cache**: Redis 7.0+ for session and data caching
- **Container**: Docker + Docker Compose for local dev; Azure Container Registry for production
- **CI/CD**: GitHub Actions for automated testing and deployment
- **Infrastructure**: Azure (App Service, SQL Database, Key Vault, Log Analytics)

### Testing & Quality

- **Code Coverage**: Unit test coverage ≥80% for all business logic layers
- **Integration Tests**: Layer 1 (TestContainers), Layer 2 (API smoke tests), Layer 3 (Azure integration)
- **Load Testing**: k6 with 100 concurrent users, 10-minute duration; p99 latency <500ms for APIs
- **Security Testing**: OWASP Top 10 vulnerability scan, dependency vulnerability scan (Dependabot)
- **Regression Testing**: All legacy business rules validated with acceptance tests

---

## 📊 Traceability Matrix

| REQ ID | EARS Type | BR Mapping | Category | Status | Test Case |
|---|---|---|---|---|---|
| REQ-BEN-001 | Ubiquitous | BR-BEN-001 | Beneficiary | ✅ | CPF_MODULO11_VALID |
| REQ-BEN-002 | Ubiquitous | BR-BEN-002 | Beneficiary | ✅ | NAME_SPACE_REQUIRED |
| REQ-BEN-003 | Ubiquitous | BR-BEN-003 | Beneficiary | ✅ | BIRTHDATE_RANGE_1900 |
| REQ-BEN-004 | Ubiquitous | BR-BEN-004 | Beneficiary | ✅ | STATE_DOMAIN_27_UFS |
| REQ-BEN-005 | Ubiquitous | BR-BEN-005 | Beneficiary | ✅ | STATUS_DOMAIN_ASCIД |
| REQ-PAY-001 | Event-driven | BR-PAY-001 | Payment | ✅ | NO_DUPLICATE_COMPETENCE |
| REQ-PAY-002 | Ubiquitous | BR-PAY-002 | Payment | ✅ | STATUS_DOMAIN_PGECДXR |
| REQ-PAY-003 | Optional | BR-PAY-003 | Payment | ✅ | DISCOUNT_CEILING_30PCT |
| REQ-PAY-004 | Event-driven | BR-PAY-004 | Payment | ✅ | CORRECTION_UPDATE_AUDIT |
| REQ-CALC-001 | Ubiquitous | BR-CALC-001 | Calculation | ✅ | MONTHLY_BENEFIT_FORMULA |
| REQ-CALC-002 | State-driven | BR-CALC-002 | Calculation | ✅ | DECEMBER_13TH_BONUS |
| REQ-CALC-003 | Optional | BR-CALC-003 | Calculation | ✅ | ABONO_PROGRAM_A_15PCT |
| REQ-AUD-001 | Ubiquitous | BR-AUD-001 | Audit | ✅ | IMMUTABLE_AUDIT_TRAIL |
| REQ-AUD-002 | Optional | BR-AUD-002 | Audit | ✅ | REPORT_EXCLUDE_EX_ACTIONS |
| REQ-DATA-001 | Ubiquitous | BR-DATA-001 | Data Retention | ✅ | PAYMENT_NO_PURGE_180M |
| **Legacy Subtotal** | **—** | **—** | **—** | **—** | **15 REQ** |
| REQ-UI-001 | Ubiquitous | New | UI/UX | ✅ | RESPONSIVE_320_2560px |
| REQ-UI-002 | Optional | New | UI/UX | ✅ | DARK_MODE_THEME |
| REQ-UI-003 | Ubiquitous | New | UI/UX | ✅ | WCAG_21_LEVEL_AA |
| REQ-UI-004 | Event-driven | New | UI/UX | ✅ | SEARCH_FILTER_REALTIME |
| REQ-UI-005 | Ubiquitous | New | UI/UX | ✅ | PAYMENT_HISTORY_PAGINATION |
| REQ-API-001 | Ubiquitous | New | API | ✅ | OPENAPI_30_SWAGGER |
| REQ-API-002 | Ubiquitous | New | API | ✅ | VALIDATION_ERROR_RESPONSE |
| REQ-API-003 | Ubiquitous | New | API | ✅ | PAGINATION_20_100 |
| REQ-API-004 | Ubiquitous | New | API | ✅ | FILTER_CPF_NAME_STATUS |
| REQ-SEC-001 | Ubiquitous | New | Security | ✅ | OAUTH2_ENTRAID_MFA |
| REQ-SEC-002 | Ubiquitous | New | Security | ✅ | ENCRYPTION_KEYVAULT |
| REQ-SEC-003 | Ubiquitous | New | Security | ✅ | AUDIT_LOG_ALL_ACTIONS |
| REQ-SEC-004 | Ubiquitous | New | Security | ✅ | HTTPS_TLS12_ENFORCE |
| REQ-SEC-005 | Ubiquitous | New | Security | ✅ | CORS_WHITELIST_ORIGINS |
| REQ-PERF-001 | Event-driven | New | Performance | ✅ | BATCH_1H_SLA_4.2M |
| REQ-PERF-002 | Ubiquitous | New | Performance | ✅ | API_P99_500ms |
| REQ-PERF-003 | Ubiquitous | New | Performance | ✅ | QUERY_100ms_INDEXES |
| REQ-PERF-004 | Ubiquitous | New | Performance | ✅ | CACHE_REDIS_24H_TTL |
| REQ-PERF-005 | Ubiquitous | New | Performance | ✅ | HIKARICP_10_50_POOL |
| **Modern Subtotal** | **—** | **—** | **—** | **—** | **19 REQ** |
| **GRAND TOTAL** | **—** | **—** | **—** | **—** | **34 REQ** |

---

## 📖 Glossary

**EARS**: Easy Approach to Requirements Syntax — structured format for writing unambiguous requirements

**Beneficiary (Beneficiário)**: Person eligible to receive government benefit payments

**Competence (Competência)**: Payment cycle period in AAAAMM format (e.g., 202604 for April 2026)

**Abono**: Additional bonus payment (e.g., 15% in December for program type A)

**CPF**: Cadastro de Pessoa Física — Brazilian tax registration ID (11 digits)

**Modulo-11**: Checksum algorithm used to validate CPF authenticity

**DDM**: Data Definition Module (Adabas term for table/entity definition)

**Lei 8159**: Brazilian Law on Archives requiring 10-year minimum retention of records

**Judicial Discount**: Court-ordered deduction, exempt from 30% ceiling

**Non-Judicial Discount**: Regular deduction (CPMF, income tax, etc.), subject to 30% ceiling

**Status Code**: Payment lifecycle state (P=pending, G=generated, C=confirmed, D=devolved, X=canceled, R=reversed)

---

## 🔗 References

**Stage 1 Artifacts**:
- [Business Rules Catalog](../01-arqueologia/business-rules-catalog.md)
- [Dependency Map](../01-arqueologia/dependency-map.md)
- [Mysteries Checklist](../01-arqueologia/mysteries-checklist.md)
- [Discovery Report](../01-arqueologia/discovery-report.md)

**Legacy Source Code**:
- [legacy/natural-programs/](../legacy/natural-programs/)
- [legacy/adabas-ddms/](../legacy/adabas-ddms/)

---

## ➡️ Next Steps

1. **Phase 2 (45 min)**: Add modern UI/API/Security/Performance requirements (REQ-UI-*, REQ-API-*, REQ-SEC-*, REQ-PERF-*)
2. **Phase 3 (60 min)**: Write Architecture Decision Records (ADR-001 through ADR-008)
3. **Definition of Done**: 30+ requirements, 5-8 ADRs, full traceability matrix complete

---

| Previous | Home | Next |
|:---------|:----:|-----:|
| [← Stage 1: Archaeology](../01-arqueologia/GUIDE.md) | [Kit Home](../README.md) | [ADRs (In Progress) →](ADR-TEMPLATE.md) |
