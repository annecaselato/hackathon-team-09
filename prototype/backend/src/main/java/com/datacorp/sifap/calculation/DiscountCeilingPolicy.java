package com.datacorp.sifap.calculation;

import com.datacorp.sifap.payment.Discount;
import com.datacorp.sifap.payment.DiscountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Discount ceiling policy — implements REQ-PAY-003.
 * Non-judicial discounts capped at 30% of base payment (VLR-BRUTO).
 * Judicial discounts (type J) are exempt from ceiling.
 * Traces to CALCDSCT.NSN lines 101-165.
 */
@Component
public class DiscountCeilingPolicy {

    public static final BigDecimal MAX_NON_JUDICIAL_RATE = new BigDecimal("0.30");

    /**
     * Applies the 30% ceiling rule to non-judicial discounts.
     * Returns the capped total discount amount.
     *
     * @param vlrBruto  base payment amount
     * @param discounts list of discount line items
     * @return total discount after ceiling applied
     */
    public BigDecimal apply(BigDecimal vlrBruto, List<Discount> discounts) {
        BigDecimal maxNonJudicial = vlrBruto.multiply(MAX_NON_JUDICIAL_RATE)
            .setScale(2, RoundingMode.HALF_DOWN);

        BigDecimal judicialTotal = discounts.stream()
            .filter(d -> d.getTipo() == DiscountType.J)
            .map(Discount::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal nonJudicialRequested = discounts.stream()
            .filter(d -> d.getTipo() != DiscountType.J)
            .map(Discount::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal nonJudicialApplied = nonJudicialRequested.min(maxNonJudicial);

        return judicialTotal.add(nonJudicialApplied);
    }

    /**
     * Returns whether the ceiling was actually applied (i.e., discount was truncated).
     */
    public boolean wasCeilingApplied(BigDecimal vlrBruto, List<Discount> discounts) {
        BigDecimal maxNonJudicial = vlrBruto.multiply(MAX_NON_JUDICIAL_RATE)
            .setScale(2, RoundingMode.HALF_DOWN);

        BigDecimal nonJudicialRequested = discounts.stream()
            .filter(d -> d.getTipo() != DiscountType.J)
            .map(Discount::getValor)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return nonJudicialRequested.compareTo(maxNonJudicial) > 0;
    }
}
