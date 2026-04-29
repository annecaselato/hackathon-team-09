package com.datacorp.sifap.calculation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Benefit calculation logic.
 * Implements:
 *   REQ-CALC-001 — Regular monthly benefit: Base × FR × FF × FI × FA × (1 + Reajuste)
 *   REQ-CALC-002 — December 13th bonus: Base × FR × FA (added when month=12)
 *   REQ-CALC-003 — Program A abono: regular × 0.15 (added when program=A and month=12)
 *
 * Traces to CALCBENF.NSN lines 225-257.
 */
@Component
public class BenefitCalculationService {

    /**
     * Calculates total monthly benefit including optional 13th and abono.
     *
     * @param base         base benefit amount
     * @param fr           factor regional (0.8–1.3)
     * @param ff           factor family (1.0–1.5)
     * @param fi           factor income (0.5–1.0)
     * @param fa           factor age (0.8–1.2)
     * @param reajuste     annual adjustment rate (e.g. 0.05 = 5%)
     * @param competenceMonth payment month (1–12)
     * @param codPrograma  program code (e.g. "A", "B")
     * @return total calculated benefit, rounded to 2 decimal places
     */
    public BigDecimal calculate(
        BigDecimal base,
        BigDecimal fr,
        BigDecimal ff,
        BigDecimal fi,
        BigDecimal fa,
        BigDecimal reajuste,
        int competenceMonth,
        String codPrograma) {

        // REQ-CALC-001: VLR-BENEF = BASE × FR × FF × FI × FA × (1 + Reajuste)
        BigDecimal vlrBenef = base
            .multiply(fr)
            .multiply(ff)
            .multiply(fi)
            .multiply(fa)
            .multiply(BigDecimal.ONE.add(reajuste))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal total = vlrBenef;

        if (competenceMonth == 12) {
            // REQ-CALC-002: 13th month — BASE × FR × FA
            BigDecimal vlr13 = base
                .multiply(fr)
                .multiply(fa)
                .setScale(2, RoundingMode.HALF_UP);
            total = total.add(vlr13);

            // REQ-CALC-003: Program A abono — 15% of regular benefit
            if ("A".equalsIgnoreCase(codPrograma)) {
                BigDecimal abono = vlrBenef
                    .multiply(new BigDecimal("0.15"))
                    .setScale(2, RoundingMode.HALF_UP);
                total = total.add(abono);
            }
        }

        return total;
    }

    /**
     * Convenience overload for simple cases (no 13th/abono consideration).
     */
    public BigDecimal calculateRegular(
        BigDecimal base, BigDecimal fr, BigDecimal ff,
        BigDecimal fi, BigDecimal fa, BigDecimal reajuste) {

        return base.multiply(fr).multiply(ff).multiply(fi).multiply(fa)
            .multiply(BigDecimal.ONE.add(reajuste))
            .setScale(2, RoundingMode.HALF_UP);
    }
}
