package com.datacorp.sifap.payment;

import com.datacorp.sifap.beneficiary.Beneficiary;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Payment entity — maps to PAGAMENTO DDM (FNR 152).
 * Key fields: NUM-PAGAMENTO, CPF-BENEF, ANO-MES-REF, VLR-BRUTO, VLR-LIQUIDO, SIT-PAGAMENTO.
 */
@Entity
@Table(name = "pagamentos", indexes = {
    @Index(name = "idx_pagamentos_cpf_competencia", columnList = "cpf_benef,ano_mes_ref,cod_programa"),
    @Index(name = "idx_pagamentos_status", columnList = "status")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiario_id", nullable = false)
    private Beneficiary beneficiary;

    @NotBlank
    @Column(name = "cpf_benef", nullable = false, length = 11)
    private String cpfBenef;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "Competence must be AAAAMM format")
    @Column(name = "ano_mes_ref", nullable = false, length = 6)
    private String anoMesRef;

    @Column(name = "cod_programa", length = 3)
    private String codPrograma;

    @NotNull
    @Column(name = "vlr_bruto", nullable = false, precision = 13, scale = 2)
    private BigDecimal vlrBruto;

    @Column(name = "vlr_desconto_total", precision = 13, scale = 2)
    private BigDecimal vlrDescontoTotal = BigDecimal.ZERO;

    @Column(name = "vlr_liquido", precision = 13, scale = 2)
    private BigDecimal vlrLiquido;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "status", nullable = false, length = 1, columnDefinition = "char(1)")
    private PaymentStatus status = PaymentStatus.P;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Discount> descontos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "usuario_geracao", length = 8)
    private String usuarioGeracao;

    public Payment() {}

    public Long getId() { return id; }
    public Beneficiary getBeneficiary() { return beneficiary; }
    public void setBeneficiary(Beneficiary beneficiary) { this.beneficiary = beneficiary; }
    public String getCpfBenef() { return cpfBenef; }
    public void setCpfBenef(String cpfBenef) { this.cpfBenef = cpfBenef; }
    public String getAnoMesRef() { return anoMesRef; }
    public void setAnoMesRef(String anoMesRef) { this.anoMesRef = anoMesRef; }
    public String getCodPrograma() { return codPrograma; }
    public void setCodPrograma(String codPrograma) { this.codPrograma = codPrograma; }
    public BigDecimal getVlrBruto() { return vlrBruto; }
    public void setVlrBruto(BigDecimal vlrBruto) { this.vlrBruto = vlrBruto; }
    public BigDecimal getVlrDescontoTotal() { return vlrDescontoTotal; }
    public void setVlrDescontoTotal(BigDecimal vlrDescontoTotal) { this.vlrDescontoTotal = vlrDescontoTotal; }
    public BigDecimal getVlrLiquido() { return vlrLiquido; }
    public void setVlrLiquido(BigDecimal vlrLiquido) { this.vlrLiquido = vlrLiquido; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public List<Discount> getDescontos() { return descontos; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public String getUsuarioGeracao() { return usuarioGeracao; }
    public void setUsuarioGeracao(String usuarioGeracao) { this.usuarioGeracao = usuarioGeracao; }
}
