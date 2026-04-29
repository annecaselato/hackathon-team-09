package com.datacorp.sifap.calculation;

import com.datacorp.sifap.payment.Discount;
import com.datacorp.sifap.payment.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DiscountCeilingPolicy.
 * Covers: REQ-PAY-003 (non-judicial 30% ceiling, judicial exemption).
 */
class DiscountCeilingPolicyTest {

    private final DiscountCeilingPolicy policy = new DiscountCeilingPolicy();

    @Test
    void givenNonJudicialDiscountAbove30Pct_whenApply_thenTruncatesTo30Pct() {
        // base=1000, non-judicial discount=350 → ceiling=300
        List<Discount> discounts = List.of(discount(DiscountType.C, "350"));
        BigDecimal result = policy.apply(new BigDecimal("1000"), discounts);
        assertThat(result).isEqualByComparingTo("300.00");
    }

    @Test
    void givenNonJudicialDiscountBelow30Pct_whenApply_thenNotTruncated() {
        // base=1000, non-judicial discount=250 (25%) → no ceiling
        List<Discount> discounts = List.of(discount(DiscountType.C, "250"));
        BigDecimal result = policy.apply(new BigDecimal("1000"), discounts);
        assertThat(result).isEqualByComparingTo("250.00");
    }

    @Test
    void givenJudicialDiscount_whenApply_thenExemptFromCeiling() {
        // base=1000, judicial discount=600 → allowed in full
        List<Discount> discounts = List.of(discount(DiscountType.J, "600"));
        BigDecimal result = policy.apply(new BigDecimal("1000"), discounts);
        assertThat(result).isEqualByComparingTo("600.00");
    }

    @Test
    void givenMixedDiscounts_whenApply_thenJudicialFullNonJudicialCapped() {
        // base=1000, judicial=200 + CPMF=400
        // judicial=200 (full), CPMF capped to 300 (30% of 1000)
        // total = 500
        List<Discount> discounts = List.of(
            discount(DiscountType.J, "200"),
            discount(DiscountType.C, "400")
        );
        BigDecimal result = policy.apply(new BigDecimal("1000"), discounts);
        assertThat(result).isEqualByComparingTo("500.00");
    }

    @Test
    void givenNoDiscounts_whenApply_thenReturnsZero() {
        BigDecimal result = policy.apply(new BigDecimal("1000"), List.of());
        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    void givenDiscountExactly30Pct_whenCheckCeilingApplied_thenReturnsFalse() {
        List<Discount> discounts = List.of(discount(DiscountType.C, "300"));
        assertThat(policy.wasCeilingApplied(new BigDecimal("1000"), discounts)).isFalse();
    }

    @Test
    void givenDiscountAbove30Pct_whenCheckCeilingApplied_thenReturnsTrue() {
        List<Discount> discounts = List.of(discount(DiscountType.C, "350"));
        assertThat(policy.wasCeilingApplied(new BigDecimal("1000"), discounts)).isTrue();
    }

    private Discount discount(DiscountType type, String value) {
        return new Discount(null, type, new BigDecimal(value), "test");
    }
}
