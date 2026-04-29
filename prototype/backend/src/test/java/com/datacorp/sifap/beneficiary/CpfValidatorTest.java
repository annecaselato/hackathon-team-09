package com.datacorp.sifap.beneficiary;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CpfValidator — REQ-BEN-001.
 */
class CpfValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "111.444.777-35",   // formatted, valid
        "11144477735",      // digits only, valid
        "529.982.247-25",   // another valid CPF
    })
    void givenValidCpf_whenValidate_thenReturnsTrue(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "111.444.777-36",   // wrong check digit
        "000.000.000-00",   // all same digit
        "111.111.111-11",   // all same digit
        "123.456.789-10",   // wrong check digits
        "12345",            // too short
        "1234567890123",    // too long
        "",
    })
    void givenInvalidCpf_whenValidate_thenReturnsFalse(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }

    @Test
    void givenNullCpf_whenValidate_thenReturnsFalse() {
        assertThat(CpfValidator.isValid(null)).isFalse();
    }

    @Test
    void givenFormattedCpf_whenNormalize_thenReturnsDigitsOnly() {
        assertThat(CpfValidator.normalize("111.444.777-35")).isEqualTo("11144477735");
    }
}
