package com.datacorp.sifap.audit;

/**
 * Audit action codes — maps to legacy COD-ACAO field (AUDITORIA DDM).
 * IN=insert, AL=alter, EX=delete attempt, CO=confirm, DV=divergence,
 * LG=login, LO=logout, BT=batch, ER=error, AU=audit, RE=reversal.
 */
public enum AuditAction {
    IN, // Insert
    AL, // Alter
    EX, // Delete attempt (excluded from standard report — REQ-AUD-002)
    CO, // Confirm
    DV, // Divergence
    LG, // Login
    LO, // Logout
    BT, // Batch
    ER, // Error
    AU, // Audit query
    RE  // Reversal
}
