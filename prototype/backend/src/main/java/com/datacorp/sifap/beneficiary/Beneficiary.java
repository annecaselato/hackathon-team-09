package com.datacorp.sifap.beneficiary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Beneficiary entity — maps to legacy BENEFICIARIO DDM (FNR 151).
 * Key fields: CPF-BENEF, NOME, DT-NASC, UF-BENEF, SIT-BENEF.
 */
@Entity
@Table(name = "beneficiarios", indexes = {
    @Index(name = "idx_beneficiarios_cpf", columnList = "cpf", unique = true),
    @Index(name = "idx_beneficiarios_status", columnList = "status")
})
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 11)
    @Column(name = "cpf", nullable = false, length = 11, unique = true)
    private String cpf;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotNull
    @Column(name = "dt_nascimento", nullable = false)
    private LocalDate dtNascimento;

    @NotBlank
    @Size(min = 2, max = 2)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uf", nullable = false, length = 2, columnDefinition = "char(2)")
    private String uf;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "status", nullable = false, length = 1, columnDefinition = "char(1)")
    private BeneficiaryStatus status = BeneficiaryStatus.A;

    @Size(max = 3)
    @Column(name = "cod_programa", length = 3)
    private String codPrograma;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Size(max = 8)
    @Column(name = "usuario_criacao", length = 8)
    private String usuarioCriacao;

    public Beneficiary() {}

    public Long getId() { return id; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public LocalDate getDtNascimento() { return dtNascimento; }
    public void setDtNascimento(LocalDate dtNascimento) { this.dtNascimento = dtNascimento; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public BeneficiaryStatus getStatus() { return status; }
    public void setStatus(BeneficiaryStatus status) { this.status = status; }
    public String getCodPrograma() { return codPrograma; }
    public void setCodPrograma(String codPrograma) { this.codPrograma = codPrograma; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public String getUsuarioCriacao() { return usuarioCriacao; }
    public void setUsuarioCriacao(String usuarioCriacao) { this.usuarioCriacao = usuarioCriacao; }
}
