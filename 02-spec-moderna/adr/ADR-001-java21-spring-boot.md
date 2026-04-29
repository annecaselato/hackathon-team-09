---
title: "ADR-001: Java 21 + Spring Boot 3.3 for Backend"
status: "Accepted"
date: "2026-04-29"
deciders: ["Vitor Filincowsky (Tech Lead)", "Danilo Lisboa (Backend Dev)", "Enterprise Architect"]
tags: ["stage-2", "adr", "backend", "java", "spring-boot"]
---

# ADR-001: Java 21 + Spring Boot 3.3 for Backend

**Status**: Accepted  
**Date**: 2026-04-29  
**Deciders**: Vitor Filincowsky (Tech Lead), Danilo Lisboa (Backend Dev)

---

## 📖 Context

SIFAP legacy backend is written in Natural 6.3.12 (end-of-life language) running on IBM mainframe. The modernization requires a new backend that:

- Implements 14 business rules extracted in Stage 1
- Processes 4.2M beneficiaries and ~3.8M payments/month
- Exposes RESTful APIs for the new web frontend
- Maintains compliance with Lei 8159 (10-year audit retention)
- Integrates with Azure services (Key Vault, Entra ID, Log Analytics)
- Runs in Docker containers deployable to Azure

DATACORP hackathon mandates the **target stack** as Java 21 + Spring Boot 3.3.

---

## ⚖️ Decision

We will use **Java 21 (LTS)** with **Spring Boot 3.3** as the backend technology stack for SIFAP 2.0, structured as a **modular monolith** with package-by-feature organization.

---

## 💡 Rationale

### Java 21 (LTS) Advantages

- **Virtual Threads (Project Loom)**: Enables high-concurrency HTTP handling without thread pool bottlenecks — critical for 500+ concurrent users and batch jobs
- **Records**: Native immutable DTOs reduce boilerplate for request/response models (e.g., `record BeneficiaryResponse(String cpf, String name, String status) {}`)
- **Pattern Matching**: `switch` pattern matching simplifies discount type routing in `CALCDSCT` equivalent logic
- **Sealed Classes**: Model domain status enums (PaymentStatus, BeneficiaryStatus) with compile-time exhaustiveness checks
- **LTS Until 2029**: Long-term support aligns with 3-5 year modernization lifecycle
- **Performance**: 20-30% throughput improvement vs. Java 17 for I/O-intensive workloads

### Spring Boot 3.3 Advantages

- **Spring Batch 5.x**: Production-grade framework for BATCHPGT, BATCHCON equivalent batch jobs; supports partitioned processing for 4.2M beneficiary records in parallel
- **Spring Security 6.3**: OAuth2 Resource Server integration with Azure Entra ID out-of-the-box
- **Spring Data JPA**: Hibernate 6.x with Jakarta EE 10 — clean repository pattern replacing Adabas READ/STORE/UPDATE/FIND
- **Actuator**: Built-in health checks, metrics (Micrometer + Prometheus), and distributed tracing
- **Native Compilation**: GraalVM native image reduces cold start for Azure Container Apps
- **Convention over Configuration**: Embedded Tomcat, auto-configuration reduces boilerplate vs. legacy XML-heavy Spring

### Business Rules Alignment

| Legacy Natural Pattern | Java 21 + Spring Boot Equivalent |
|---|---|
| `CALLNAT 'VALBENEF'` | `BeneficiaryValidationService.validate()` |
| `FIND BENEFICIARIO` | `BeneficiaryRepository.findByCpf()` |
| `STORE PAGAMENTO` | `PaymentRepository.save()` |
| `AT BREAK` (batch control) | `Spring Batch ItemProcessor<>` |
| `ON ERROR` block | `@ExceptionHandler` / `GlobalExceptionHandler` |
| Adabas DDM | JPA `@Entity` with `@Table` |

---

## 📊 Consequences

### Positive

- **Talent pool**: Large global pool of Java/Spring developers; easier hiring for DATACORP
- **Ecosystem maturity**: Spring ecosystem has solutions for every integration (Azure, PostgreSQL, Redis, Kafka)
- **Testability**: JUnit 5 + Mockito + TestContainers enable all 3 layers of integration testing
- **Performance**: Virtual threads + Spring Batch partitioning will achieve <1h batch SLA (vs. legacy 1.5-3.3h)
- **Observability**: Spring Actuator + Micrometer provides built-in metrics for Azure Monitor

### Negative / Trade-offs

- **Learning curve**: Natural → Java paradigm shift for team members; addressed by Copilot-assisted code generation
- **JVM startup time**: Mitigated by container warm-up strategy and GraalVM native option for batch jobs
- **Verbose compared to Kotlin**: Accepted; records/pattern matching in Java 21 significantly reduce verbosity

---

## 🔀 Alternatives Considered

### Alternative A: Kotlin + Spring Boot
- **Pros**: More concise syntax, coroutines for async, null safety
- **Rejected because**: Team standardized on Java; Kotlin interoperability complexity for new team members; DATACORP stack mandate is Java 21

### Alternative B: Quarkus + Java 21
- **Pros**: Faster native compilation, lower memory footprint
- **Rejected because**: Smaller ecosystem, less Spring Batch equivalent maturity, DATACORP mandate specifies Spring Boot

### Alternative C: Python + FastAPI
- **Pros**: Rapid development, excellent data processing libraries
- **Rejected because**: Not DATACORP standard; Spring Batch has no equivalent for complex batch processing; Java required for JPA/Hibernate

### Alternative D: Microservices (one service per domain)
- **Pros**: Independent scaling, team autonomy
- **Rejected because**: Premature for team size and timeline; network latency between services for payment calculation; see ADR-004 for architecture pattern decision

---

## 🛡️ Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Batch SLA not met (<1h) | MEDIUM | HIGH | Spring Batch partitioning + JPA batch insert (50 records/chunk) + VT (virtual threads) |
| Natural → Java business rule translation errors | MEDIUM | HIGH | Stage 1 BR-* with line-level traceability; acceptance tests for every rule |
| Java 21 version compatibility issues with Azure | LOW | MEDIUM | Azure App Service and Container Apps support Java 21 (verified) |

---

## 🔗 References

- [Spring Boot 3.3 Release Notes](https://spring.io/blog/2024/05/23/spring-boot-3-3-0)
- [Java 21 LTS Virtual Threads (JEP 444)](https://openjdk.org/jeps/444)
- [Spring Batch 5.x Documentation](https://docs.spring.io/spring-batch/reference/index.html)
- [SIFAP BATCHPGT Logic](../legacy/natural-programs/BATCHPGT.NSN)

---

## ✅ Approval

| Role | Name | Status |
|---|---|---|
| Tech Lead | Vitor Filincowsky | ✅ Approved |
| Backend Developer | Danilo Lisboa | ✅ Approved |
| Enterprise Architect | (Stage 2 review) | 🔄 Pending |
