package com.datacorp.sifap.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public record PaymentRequest(
    @NotBlank String cpfBenef,
    @NotBlank @Pattern(regexp = "\\d{6}") String anoMesRef,
    String codPrograma,
    @NotNull BigDecimal vlrBruto,
    List<DiscountLineRequest> descontos
) {
    public record DiscountLineRequest(
        @NotNull DiscountType tipo,
        @NotNull BigDecimal valor,
        String descricao
    ) {}
}
