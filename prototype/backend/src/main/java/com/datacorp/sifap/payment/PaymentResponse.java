package com.datacorp.sifap.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(
    Long id,
    String cpfBenef,
    String anoMesRef,
    String codPrograma,
    BigDecimal vlrBruto,
    BigDecimal vlrDescontoTotal,
    BigDecimal vlrLiquido,
    PaymentStatus status,
    List<DiscountResponse> descontos,
    LocalDateTime criadoEm
) {
    public record DiscountResponse(Long id, DiscountType tipo, BigDecimal valor, String descricao) {}

    public static PaymentResponse from(Payment p) {
        List<DiscountResponse> discounts = p.getDescontos().stream()
            .map(d -> new DiscountResponse(d.getId(), d.getTipo(), d.getValor(), d.getDescricao()))
            .toList();
        return new PaymentResponse(
            p.getId(), p.getCpfBenef(), p.getAnoMesRef(), p.getCodPrograma(),
            p.getVlrBruto(), p.getVlrDescontoTotal(), p.getVlrLiquido(),
            p.getStatus(), discounts, p.getCriadoEm()
        );
    }
}
