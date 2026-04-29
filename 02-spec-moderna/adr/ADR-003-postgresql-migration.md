---
title: "ADR-003: PostgreSQL 16 to Replace Adabas 7.4.3"
status: "Accepted"
date: "2026-04-29"
deciders: ["Vitor Filincowsky (Tech Lead)", "Danilo Lisboa (Backend Dev)", "DBA (Stage 2 review)"]
tags: ["stage-2", "adr", "database", "postgresql", "adabas", "migration"]
---

# ADR-003: PostgreSQL 16 to Replace Adabas 7.4.3

**Status**: Accepted  
**Date**: 2026-04-29  
**Deciders**: Vitor Filincowsky (Tech Lead), Danilo Lisboa (Backend Dev)

---

## 📖 Context

Current database is **Adabas 7.4.3** (Software AG's inverted-file DBMS, designed for IBM mainframes). Issues:

- **Cost**: Adabas licensing on IBM mainframe is significant cost driver
- **End of support**: Adabas 7.x reached Extended Maintenance in 2023
- **Integration difficulty**: No native JDBC/JPA connectors; requires Natural Studio or custom ADALNK bridge
- **Schema evolution**: DDMs (Data Definition Modules) require mainframe access to modify; no migration tooling
- **Volume concern**: PAGAMENTO table has 180M+ records; Adabas sequential reads degrade with scale

**Data to migrate** (Stage 1 archaeology findings):
- BENEFICIARIO (FNR 151): ~4.2M records, ~850B avg → ~3.5GB
- PAGAMENTO (FNR 152): 180M+ records, ~720B avg → ~130GB (compressed ~40GB)
- AUDITORIA (FNR 153): 25M+ records, ~1.2KB avg → ~30GB
- PROGRAMA-SOCIAL (FNR 154): ~150 records → <1MB

DATACORP mandates: **PostgreSQL 16**

---

## ⚖️ Decision

We will use **PostgreSQL 16** as the target database, replacing Adabas 7.4.3, with the following design:
- **Table partitioning** for PAGAMENTO by year-month (to maintain query performance across 180M+ records)
- **pgcrypto** for field-level encryption of sensitive data (CPF, bank account)
- **Row-level security** for multi-tenant data access control
- **pg_audit** extension for database-level audit logging

---

## 💡 Rationale

### PostgreSQL 16 Advantages

- **Open source**: Zero licensing cost vs. Adabas mainframe license
- **ACID compliance**: Full ACID transactions (consistent with Adabas behavior), plus MVCC for concurrent reads without locking
- **JPA/Hibernate 6.x**: Native support via Spring Data JPA; replaces all Adabas READ/FIND/STORE/UPDATE/DELETE Natural statements with repository pattern
- **Table partitioning**: `PARTITION BY RANGE (ano_mes_ref)` on PAGAMENTO avoids sequential scans on 180M+ records; each month becomes a separate partition
- **pgcrypto**: Built-in `pgp_sym_encrypt()` for CPF field encryption (REQ-SEC-002)
- **Full-text search**: `pg_trgm` extension enables fast name search (REQ-API-004)
- **JSON/JSONB**: Audit trail GRP-ANTES/GRP-DEPOIS (old/new values) can be stored as JSONB — preserves MU field semantics from Adabas
- **Azure Database for PostgreSQL**: Managed service eliminates DBA overhead; automatic backups, patching, HA

### Adabas → PostgreSQL DDM Mapping

| Adabas DDM | PostgreSQL Table | Key Design |
|---|---|---|
| BENEFICIARIO (FNR 151) | `beneficiarios` | PK: CPF (VARCHAR 11); indexes on (cpf, status) |
| PAGAMENTO (FNR 152) | `pagamentos` PARTITIONED | PK: num_pagamento; partitioned by ano_mes_ref (YYYYMM); indexes on (cpf_benef, ano_mes_ref, sit_pagamento) |
| AUDITORIA (FNR 153) | `auditoria` | PK: num_auditoria; immutable (no UPDATE/DELETE); indexes on (dt_evento, cod_acao, tipo_entidade) |
| PROGRAMA-SOCIAL (FNR 154) | `programas_sociais` | PK: cod_programa; cached in Redis |
| GRP-DESCONTO (PE group) | `descontos` (child table) | FK: num_pagamento; max 8 rows per payment |

### Adabas Type Mapping

| Adabas Type | Example Field | PostgreSQL Type |
|---|---|---|
| A (Alpha) | NOME (A100) | VARCHAR(100) |
| N (Numeric) | VLR-BRUTO (N9.2) | NUMERIC(9,2) |
| P (Packed decimal) | VLR-DESCONTO | NUMERIC(7,2) |
| N (Date YYYYMMDD) | DT-NASCIMENTO | DATE (converted on migration) |
| MU (Multiple value) | GRP-ANTES | JSONB |
| PE (Periodic group) | GRP-DESCONTO | Child table (FK) |

---

## 📊 Consequences

### Positive

- **Cost reduction**: Eliminates Adabas mainframe license (~40-60% DB cost reduction)
- **Performance**: Partitioned PAGAMENTO table maintains <50ms queries even at 500M+ records
- **Developer experience**: JPA/Hibernate replaces FIND/STORE Natural statements; no custom ADALNK bridge needed
- **Tooling**: pgAdmin, DBeaver, standard SQL tools; vs. Natural Studio mainframe-only tooling
- **Azure integration**: Azure Database for PostgreSQL Flexible Server: auto-backup, geo-redundancy, scale-up without downtime
- **Migration safety**: pg_restore allows point-in-time validation during migration

### Negative / Trade-offs

- **Migration complexity**: 180M+ records require multi-phase ETL; cannot do big-bang cutover
- **Date format conversion**: Adabas N8 (YYYYMMDD) → PostgreSQL DATE requires conversion ETL; validated in migration tests
- **MU/PE field restructuring**: Adabas multiple-value fields → normalized SQL tables; semantic equivalence must be tested
- **GRP-ANTES/GRP-DEPOIS as JSONB**: Trade-off between normalized structure and flexibility; chosen for audit trail evolution

---

## 🔀 Alternatives Considered

### Alternative A: Azure SQL Database (SQL Server)
- **Pros**: Azure-native, familiar to some developers, strong enterprise support
- **Rejected because**: DATACORP mandate is PostgreSQL; licensing cost vs. open source; JSON support less mature than PostgreSQL JSONB

### Alternative B: MongoDB (document store)
- **Pros**: Schema flexibility for varying audit fields, natural fit for JSONB-like data
- **Rejected because**: ACID transactions critical for payment processing; JPA/Hibernate support requires extra complexity; DATACORP mandate is relational

### Alternative C: Keep Adabas + add API layer
- **Pros**: No migration risk
- **Rejected because**: Mainframe cost driver; no JPA/JDBC support; scalability ceiling; operational overhead; end-of-support

### Alternative D: MariaDB / MySQL
- **Pros**: Popular, open source, Azure support
- **Rejected because**: DATACORP mandate is PostgreSQL; weaker partitioning; JSONB support inferior; pgcrypto not available

---

## 🛡️ Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Data loss during 180M record migration | LOW | CRITICAL | Multi-phase migration with reconciliation reports; Adabas retained as backup 90 days |
| Date format conversion errors (N8 → DATE) | MEDIUM | HIGH | ETL validation step: count mismatches; test with 1% sample before full run |
| Performance regression on PAGAMENTO queries | MEDIUM | HIGH | Partition pruning tested before cutover; EXPLAIN ANALYZE on critical queries |
| Audit immutability broken | LOW | CRITICAL | `REVOKE DELETE, UPDATE ON auditoria FROM app_user`; trigger blocks all modifications |
| N+1 query problems with JPA | MEDIUM | MEDIUM | Fetch join strategy in repositories; Hibernate batch mode; Datadog APM alerts |

---

## 🔗 References

- [PostgreSQL 16 Partitioning Documentation](https://www.postgresql.org/docs/16/ddl-partitioning.html)
- [pgcrypto Functions](https://www.postgresql.org/docs/16/pgcrypto.html)
- [PAGAMENTO DDM Analysis](../legacy/adabas-ddms/PAGAMENTO.ddm)
- [AUDITORIA DDM Analysis](../legacy/adabas-ddms/AUDITORIA.ddm)
- [REQ-DATA-001 (No Purge Policy)](../SPECIFICATION.md#req-data-001)
- [REQ-SEC-002 (Encryption at Rest)](../SPECIFICATION.md#req-sec-002)

---

## ✅ Approval

| Role | Name | Status |
|---|---|---|
| Tech Lead | Vitor Filincowsky | ✅ Approved |
| Backend Developer | Danilo Lisboa | ✅ Approved |
| DBA | (Stage 2 review) | 🔄 Pending |
