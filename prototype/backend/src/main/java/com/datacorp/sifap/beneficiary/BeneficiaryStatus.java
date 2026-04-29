package com.datacorp.sifap.beneficiary;

/**
 * Beneficiary status codes — maps to legacy VALBENEF.NSN line 164.
 * A=active, S=suspended, C=cancelled, I=inactive, D=deleted
 */
public enum BeneficiaryStatus {
    A, // Active
    S, // Suspended
    C, // Cancelled
    I, // Inactive
    D  // Deleted
}
