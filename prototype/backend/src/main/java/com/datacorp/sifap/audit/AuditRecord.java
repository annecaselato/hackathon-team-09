package com.datacorp.sifap.audit;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Immutable audit record — maps to AUDITORIA DDM (FNR 153).
 * Implements REQ-AUD-001: append-only, 10-year retention.
 *
 * DB-level immutability enforced via:
 *   REVOKE DELETE, UPDATE ON auditoria FROM sifap_app;
 *   RULE auditoria_no_delete (see V1__schema.sql)
 *
 * sistema_origem: 'L' = legacy Natural, 'M' = modern SIFAP 2.0
 */
@Entity
@Immutable
@Table(name = "auditoria", indexes = {
    @Index(name = "idx_auditoria_entidade", columnList = "tipo_entidade,id_entidade"),
    @Index(name = "idx_auditoria_dt_evento", columnList = "dt_evento"),
    @Index(name = "idx_auditoria_cod_acao", columnList = "cod_acao")
})
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dt_evento", nullable = false)
    private LocalDate dtEvento;

    @Column(name = "hr_evento", nullable = false)
    private LocalTime hrEvento;

    @Column(name = "ts_evento", nullable = false)
    private OffsetDateTime tsEvento;

    @Enumerated(EnumType.STRING)
    @Column(name = "cod_acao", nullable = false, length = 2)
    private AuditAction codAcao;

    @Column(name = "cod_modulo", nullable = false, length = 50)
    private String codModulo;

    @Column(name = "tipo_entidade", nullable = false, length = 4)
    private String tipoEntidade;

    @Column(name = "id_entidade", nullable = false, length = 15)
    private String idEntidade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "campos_antes", columnDefinition = "jsonb")
    private String camposAntes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "campos_depois", columnDefinition = "jsonb")
    private String camposDepois;

    @Column(name = "usr_evento", nullable = false, length = 8)
    private String usrEvento;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sistema_origem", nullable = false, length = 1, columnDefinition = "char(1)")
    private String sistemaOrigem = "M";

    public AuditRecord() {}

    public Long getId() { return id; }
    public LocalDate getDtEvento() { return dtEvento; }
    public void setDtEvento(LocalDate dtEvento) { this.dtEvento = dtEvento; }
    public LocalTime getHrEvento() { return hrEvento; }
    public void setHrEvento(LocalTime hrEvento) { this.hrEvento = hrEvento; }
    public OffsetDateTime getTsEvento() { return tsEvento; }
    public void setTsEvento(OffsetDateTime tsEvento) { this.tsEvento = tsEvento; }
    public AuditAction getCodAcao() { return codAcao; }
    public void setCodAcao(AuditAction codAcao) { this.codAcao = codAcao; }
    public String getCodModulo() { return codModulo; }
    public void setCodModulo(String codModulo) { this.codModulo = codModulo; }
    public String getTipoEntidade() { return tipoEntidade; }
    public void setTipoEntidade(String tipoEntidade) { this.tipoEntidade = tipoEntidade; }
    public String getIdEntidade() { return idEntidade; }
    public void setIdEntidade(String idEntidade) { this.idEntidade = idEntidade; }
    public String getCamposAntes() { return camposAntes; }
    public void setCamposAntes(String camposAntes) { this.camposAntes = camposAntes; }
    public String getCamposDepois() { return camposDepois; }
    public void setCamposDepois(String camposDepois) { this.camposDepois = camposDepois; }
    public String getUsrEvento() { return usrEvento; }
    public void setUsrEvento(String usrEvento) { this.usrEvento = usrEvento; }
    public String getSistemaOrigem() { return sistemaOrigem; }
    public void setSistemaOrigem(String sistemaOrigem) { this.sistemaOrigem = sistemaOrigem; }
}
