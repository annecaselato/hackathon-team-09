package com.datacorp.sifap.payment;

/**
 * Discount type codes — maps to legacy GRP-DESCONTO.TP-DESCONTO (PAGAMENTO DDM).
 * J=Judicial (exempt from 30% ceiling), C=CPMF, I=Income Tax, O=Other.
 * Implements REQ-PAY-003.
 */
public enum DiscountType {
    J, // Judicial — court order (exempt from 30% ceiling per BR-PAY-003)
    C, // CPMF — Contribuição Provisória sobre Movimentação Financeira
    I, // Income tax — Imposto de Renda
    O  // Other
}
