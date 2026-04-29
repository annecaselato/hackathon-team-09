package com.datacorp.sifap.payment;

/**
 * Payment status domain — maps to PAGAMENTO.ddm line 48 (SIT-PAGAMENTO A1).
 * Implements REQ-PAY-002.
 * P=Pending, G=Generated, E=Error, C=Confirmed, D=Devolved, X=Canceled, R=Reversed
 */
public enum PaymentStatus {
    P, // Pending — awaiting dispatch
    G, // Generated — ready for dispatch
    E, // Error — processing failed
    C, // Confirmed — bank confirmed receipt
    D, // Devolved — bank returned unpaid
    X, // Canceled — manually canceled
    R  // Reversed — post-payment reversal
}
