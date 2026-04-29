package com.datacorp.sifap.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Audit trail service — implements REQ-AUD-001.
 * Records are always appended, never updated or deleted.
 * Uses REQUIRES_NEW propagation so audit always commits even if caller rolls back.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Appends an audit record.
     *
     * @param action       action code (IN, AL, EX, etc.)
     * @param entityType   4-char entity type (BENF, PGTO, PROG, ADMN)
     * @param entityId     entity primary key as string
     * @param before       old state (null for inserts)
     * @param after        new state
     * @param userId       operator user ID (max 8 chars)
     * @param sourceModule source class/module name
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
        AuditAction action,
        String entityType,
        String entityId,
        Object before,
        Object after,
        String userId,
        String sourceModule) {

        OffsetDateTime now = OffsetDateTime.now();

        AuditRecord rec = new AuditRecord();
        rec.setDtEvento(LocalDate.now());
        rec.setHrEvento(LocalTime.now());
        rec.setTsEvento(now);
        rec.setCodAcao(action);
        rec.setCodModulo(sourceModule);
        rec.setTipoEntidade(entityType);
        rec.setIdEntidade(entityId);
        rec.setUsrEvento(userId != null ? userId.substring(0, Math.min(userId.length(), 8)) : "SYSTEM");
        rec.setSistemaOrigem("M");

        if (before != null) rec.setCamposAntes(toJson(before));
        if (after != null) rec.setCamposDepois(toJson(after));

        repository.save(rec);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit object: {}", e.getMessage());
            return "{\"error\":\"serialization_failed\"}";
        }
    }
}
