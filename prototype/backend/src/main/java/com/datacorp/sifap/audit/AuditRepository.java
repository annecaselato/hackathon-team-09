package com.datacorp.sifap.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface AuditRepository extends JpaRepository<AuditRecord, Long> {

    /**
     * REQ-AUD-002: standard report excludes EX (delete attempts).
     */
    @Query("SELECT a FROM AuditRecord a WHERE a.codAcao <> 'EX' " +
           "AND (:from IS NULL OR a.dtEvento >= :from) " +
           "AND (:to IS NULL OR a.dtEvento <= :to) " +
           "ORDER BY a.tsEvento DESC")
    Page<AuditRecord> findStandardReport(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        Pageable pageable);

    /** Full report — includes EX (used only when explicitly requested). */
    @Query("SELECT a FROM AuditRecord a WHERE " +
           "(:from IS NULL OR a.dtEvento >= :from) " +
           "AND (:to IS NULL OR a.dtEvento <= :to) " +
           "ORDER BY a.tsEvento DESC")
    Page<AuditRecord> findFullReport(
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        Pageable pageable);

    Page<AuditRecord> findByTipoEntidadeAndIdEntidade(
        String tipoEntidade, String idEntidade, Pageable pageable);
}
