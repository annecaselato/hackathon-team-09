---
title: "ADR-006: Data Migration Strategy for 180M+ Legacy Records"
status: "Accepted"
date: "2026-04-29"
deciders: ["Danilo Lisboa (Backend Dev)", "Anne Caselato (DevOps)", "DBA (Stage 2 review)"]
tags: ["stage-2", "adr", "migration", "data", "pagamento", "etl"]
---

# ADR-006: Data Migration Strategy for 180M+ Legacy Records

**Status**: Accepted  
**Date**: 2026-04-29  
**Deciders**: Danilo Lisboa (Backend Dev), Anne Caselato (DevOps)

---

## 📖 Context

Stage 1 archaeology revealed critical data retention facts:

| Entity | Records | Size (approx) | Retention Policy |
|---|---|---|---|
| BENEFICIARIO (FNR 151) | 4.2M | ~3.5GB | Active; no purge |
| PAGAMENTO (FNR 152) | 180M+ | ~130GB raw / ~40GB compressed | **No purge since 1998** (28 years) |
| AUDITORIA (FNR 153) | 25M+ | ~30GB | **Immutable** since 2005; 10-year legal minimum |
| PROGRAMA-SOCIAL (FNR 154) | ~150 | <1MB | Parametrization table |

**Critical constraints**:
- PAGAMENTO has **no cutoff date** — all 28 years must be migrated (BR-DATA-001)
- AUDITORIA records are **legally immutable** — migration must not alter any field (BR-AUD-001)
- Migration must be **non-destructive** — legacy system retained as fallback for 90 days post-cutover
- Bank hashes (`HASH-ARQ-REMESSA`, `HASH-ARQ-RETORNO` — SHA-256, added 2015) must be preserved exactly

**Risk**: A failed migration with data loss or corruption is a compliance violation under Lei 8159.

---

## ⚖️ Decision

We will use a **multi-phase, non-destructive migration strategy** with a parallel run validation period:

### Phase 1: Schema Preparation (Week 1-2)
- Design PostgreSQL schema with partitioning, indexes, and constraints
- Create ETL scripts (Python + psycopg2 or Apache Spark) for each DDM
- Run migration on 1% sample (~1.8M PAGAMENTO records); validate row counts and value checksums

### Phase 2: Initial Load — Historical Data (Week 3-4)
- **BENEFICIARIO**: Full load (4.2M records) — single batch, <2 hours
- **PROGRAMA-SOCIAL**: Full load (150 records) — immediate
- **PAGAMENTO**: Chunked historical load by year (1998-2024); load older data first (less critical, establishes baseline)
- **AUDITORIA**: Chunked load by year (2005-2024); read-only after migration

### Phase 3: Delta Sync (Parallel Run — 1 Month)
- Legacy system continues operating as source of truth
- Modern system mirrors writes via **CDC (Change Data Capture)** or batch delta jobs every 15 minutes
- Comparison dashboard shows counts and totals match between legacy and modern

### Phase 4: Validation & Reconciliation
- **Row count match**: Every entity exact count match
- **Financial reconciliation**: Sum of VLR-BRUTO, VLR-LIQUIDO, VLR-DESCONTO for each AAAAMM competence match exactly
- **Audit completeness**: All AUDITORIA records present with no field modification
- **Hash integrity**: SHA-256 hashes on PAGAMENTO (added 2015) verified post-migration

### Phase 5: Cutover (go-live)
- Phased cutover: 5% beneficiaries → 25% → 100% (over 3 deployment cycles)
- Legacy SIFAP remains operational as read-only fallback for 90 days
- Rollback: If critical issue detected, traffic routed back to legacy within 4 hours (RTO)

---

## 💡 Rationale

### Why Multi-Phase (not Big-Bang)

- **Risk reduction**: 180M records cannot be validated in a single pass; phased approach allows course correction
- **Business continuity**: Legacy system remains operational during migration; zero downtime for operators
- **Financial compliance**: Monthly payment cycles cannot be disrupted; cutover scheduled after monthly cycle completion

### Why Partition-First

- PAGAMENTO must be partitioned by `ano_mes_ref` (YYYYMM) **before** loading 180M records; adding partitioning after full load requires table rebuild (~8 hours downtime)
- Each year's partition (1998-2026 = 29 partitions × 12 months = 348 partitions) allows pruning of irrelevant date ranges in queries

### Why CDC for Delta Sync

- Adabas native CDC requires Software AG Replication Server — available on our version
- Alternative: Batch delta jobs comparing `DT-ATUALIZACAO` timestamps every 15 minutes — simpler, sufficient for 1-month parallel run

### Migration ETL Design

```
Adabas Extract (Natural ADALNK / Adabas ADADBS utility)
    ↓
CSV export (compressed .gz)
    ↓
Python ETL (validation, type conversion, enrichment)
    ├─ N8 (YYYYMMDD) → DATE conversion + validation
    ├─ N (packed decimal) → NUMERIC(9,2)
    ├─ MU fields → JSONB arrays (GRP-ANTES/GRP-DEPOIS)
    └─ PE groups → child table rows (GRP-DESCONTO)
    ↓
PostgreSQL COPY (bulk insert, 10x faster than INSERT)
    ↓
Validation (counts, sums, hash verification)
```

---

## 📊 Consequences

### Positive

- **Zero data loss**: Phased approach with validation at each stage
- **Rollback capability**: Legacy system operational for 90 days post-cutover
- **Financial accuracy**: VLR-BRUTO/LIQUIDO reconciliation per competence ensures exact match
- **Compliance**: AUDITORIA records migrated without modification; immutability preserved

### Negative / Trade-offs

- **Duration**: Full migration timeline is 4-6 weeks; not instant
- **ETL maintenance**: Python ETL scripts must be maintained for parallel run period
- **Dual-write complexity**: During parallel run, writes must go to both systems (or CDC handles it)
- **Storage**: Temporary 40GB+ of CSV export files during migration; cleaned up post-cutover

---

## 🔀 Alternatives Considered

### Alternative A: Big-Bang Migration (single weekend)
- **Pros**: Simpler process, clean cutover
- **Rejected because**: 180M records cannot be migrated, validated, and go-live tested in a weekend; risk of data loss or corruption unacceptable

### Alternative B: Keep Legacy as Archive, Modern Only for New Data
- **Pros**: No migration risk for historical data
- **Rejected because**: Compliance requires querying historical payments (1998-2026) in operational system; operators need unified view; two systems defeats modernization goal

### Alternative C: Azure Database Migration Service
- **Pros**: Managed migration tool from Microsoft
- **Rejected because**: No native Adabas connector; requires ODBC bridge; adds tooling dependency; Python ETL gives more control for custom type conversions

---

## 🛡️ Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Data loss during 180M record export | LOW | CRITICAL | Checksums before and after each phase; Adabas backup before any extraction |
| Type conversion errors (N8 → DATE) | MEDIUM | HIGH | Sample validation on 1% before full run; fail-fast on conversion error |
| AUDITORIA fields modified during migration | LOW | CRITICAL | ETL audit: compare hash of each migrated AUDITORIA row vs. original |
| Partition performance worse than expected | MEDIUM | HIGH | Load test on partitioned schema with 200M test records before production migration |
| Legacy cutoff decision point delayed | MEDIUM | MEDIUM | Define explicit go/no-go criteria; signed off by Tech Lead + Compliance |

---

## 🔗 References

- [PAGAMENTO DDM (180M+ records, no purge)](../legacy/adabas-ddms/PAGAMENTO.ddm)
- [AUDITORIA DDM (immutable, Lei 8159)](../legacy/adabas-ddms/AUDITORIA.ddm)
- [BR-DATA-001 (No Purge Policy)](../01-arqueologia/business-rules-catalog.md)
- [REQ-DATA-001 (Payment Records Retention)](../SPECIFICATION.md#req-data-001)
- [PostgreSQL COPY Documentation](https://www.postgresql.org/docs/16/sql-copy.html)

---

## ✅ Approval

| Role | Name | Status |
|---|---|---|
| Backend Developer | Danilo Lisboa | ✅ Approved |
| DevOps Engineer | Anne Caselato | ✅ Approved |
| DBA | (Stage 2 review) | 🔄 Pending |
