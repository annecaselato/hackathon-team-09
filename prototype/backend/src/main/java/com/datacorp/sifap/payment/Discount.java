package com.datacorp.sifap.payment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * Discount line item — maps to GRP-DESCONTO periodic group (PE) in PAGAMENTO DDM.
 * Max 8 rows per payment (Adabas PE max occurrences).
 */
@Entity
@Table(name = "descontos")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagamento_id", nullable = false)
    private Payment payment;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tp_desconto", nullable = false, length = 1, columnDefinition = "char(1)")
    private DiscountType tipo;

    @NotNull
    @Column(name = "vlr_desconto", nullable = false, precision = 9, scale = 2)
    private BigDecimal valor;

    @Column(name = "descricao", length = 60)
    private String descricao;

    public Discount() {}

    public Discount(Payment payment, DiscountType tipo, BigDecimal valor, String descricao) {
        this.payment = payment;
        this.tipo = tipo;
        this.valor = valor;
        this.descricao = descricao;
    }

    public Long getId() { return id; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public DiscountType getTipo() { return tipo; }
    public void setTipo(DiscountType tipo) { this.tipo = tipo; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
