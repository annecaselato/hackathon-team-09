package com.datacorp.sifap.payment;

import java.math.BigDecimal;

public record PaymentCorrectionRequest(
    BigDecimal vlrBruto,
    PaymentStatus status
) {}
