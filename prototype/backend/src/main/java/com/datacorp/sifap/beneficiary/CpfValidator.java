package com.datacorp.sifap.beneficiary;

/**
 * CPF validation using modulo-11 algorithm.
 * Implements BR-BEN-001 — traces to VALBENEF.NSN lines 113-236.
 *
 * Algorithm:
 *  1st check digit: weights 10..2 applied to digits 1-9, mod 11, remainder.
 *  2nd check digit: weights 11..2 applied to digits 1-10, mod 11, remainder.
 *  Remainder < 2 → digit = 0; else digit = 11 - remainder.
 */
public final class CpfValidator {

    private static final int CPF_LENGTH = 11;

    private CpfValidator() {}

    /**
     * Validates a CPF string (digits only, no formatting).
     *
     * @param cpf 11-digit CPF string
     * @return true if CPF passes modulo-11 validation
     */
    public static boolean isValid(String cpf) {
        if (cpf == null) return false;

        String digits = cpf.replaceAll("[^0-9]", "");

        if (digits.length() != CPF_LENGTH) return false;

        // Reject all-same-digit CPFs (e.g., 000.000.000-00, 111.111.111-11)
        if (digits.chars().distinct().count() == 1) return false;

        int firstDigit = calculateCheckDigit(digits, 10);
        if (firstDigit != Character.getNumericValue(digits.charAt(9))) return false;

        int secondDigit = calculateCheckDigit(digits, 11);
        return secondDigit == Character.getNumericValue(digits.charAt(10));
    }

    private static int calculateCheckDigit(String digits, int initialWeight) {
        int sum = 0;
        for (int i = 0; i < initialWeight - 1; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * (initialWeight - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    /**
     * Normalizes CPF to digits-only format (removes dots and dash).
     */
    public static String normalize(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("[^0-9]", "");
    }
}
