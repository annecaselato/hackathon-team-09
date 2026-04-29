package com.datacorp.sifap.calculation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BenefitCalculationService.
 * Covers: REQ-CALC-001, REQ-CALC-002, REQ-CALC-003.
 */
class BenefitCalculationServiceTest {

    private final BenefitCalculationService service = new BenefitCalculationService();

    // ---- REQ-CALC-001: Regular monthly benefit ----

    @Test
    void givenStandardFactors_whenCalculate_thenAppliesFormulaCorrectly() {
        // base=1000, FR=1.1, FF=1.0, FI=1.0, FA=1.0, reajuste=0.05, month=3
        BigDecimal result = service.calculate(
            bd("1000"), bd("1.1"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.05"),
            3, "B");

        // 1000 × 1.1 × 1.0 × 1.0 × 1.0 × 1.05 = 1155.00
        assertThat(result).isEqualByComparingTo("1155.00");
    }

    @Test
    void givenDecimalResult_whenCalculate_thenRoundsToTwoDecimalPlaces() {
        // Produces non-terminating decimal
        BigDecimal result = service.calculateRegular(
            bd("1000"), bd("1.0"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.001"));
        // 1000 × 1.001 = 1001.00
        assertThat(result.scale()).isEqualTo(2);
    }

    // ---- REQ-CALC-002: December 13th bonus ----

    @Test
    void givenDecemberPayment_whenCalculate_thenAdds13thBonus() {
        // base=1000, FR=1.1, FF=1.0, FI=1.0, FA=1.0, reajuste=0.0, month=12, program=B
        BigDecimal result = service.calculate(
            bd("1000"), bd("1.1"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.0"),
            12, "B");

        // Regular = 1000 × 1.1 = 1100.00
        // 13th    = 1000 × 1.1 × 1.0 = 1100.00
        // Total   = 2200.00
        assertThat(result).isEqualByComparingTo("2200.00");
    }

    @Test
    void givenNonDecemberPayment_whenCalculate_thenNoBonus() {
        BigDecimal dec = service.calculate(
            bd("1000"), bd("1.0"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.0"),
            12, "B");
        BigDecimal jan = service.calculate(
            bd("1000"), bd("1.0"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.0"),
            1, "B");

        assertThat(dec).isGreaterThan(jan);
    }

    // ---- REQ-CALC-003: Program A abono (15% December bonus) ----

    @Test
    void givenProgramAInDecember_whenCalculate_thenAddsAbono() {
        // base=1000, all factors=1.0, reajuste=0.0, month=12, program=A
        BigDecimal result = service.calculate(
            bd("1000"), bd("1.0"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.0"),
            12, "A");

        // Regular   = 1000.00
        // 13th      = 1000.00
        // Abono 15% = 1000 × 0.15 = 150.00
        // Total     = 2150.00
        assertThat(result).isEqualByComparingTo("2150.00");
    }

    @Test
    void givenProgramBInDecember_whenCalculate_thenNoAbono() {
        BigDecimal programA = service.calculate(
            bd("1000"), bd("1.0"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.0"),
            12, "A");
        BigDecimal programB = service.calculate(
            bd("1000"), bd("1.0"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.0"),
            12, "B");

        assertThat(programA).isGreaterThan(programB);
    }

    @Test
    void givenProgramAInJanuary_whenCalculate_thenNoAbono() {
        BigDecimal janA = service.calculate(
            bd("1000"), bd("1.0"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.0"),
            1, "A");
        BigDecimal janB = service.calculate(
            bd("1000"), bd("1.0"), bd("1.0"), bd("1.0"), bd("1.0"), bd("0.0"),
            1, "B");

        assertThat(janA).isEqualByComparingTo(janB);
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
