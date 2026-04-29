package com.datacorp.sifap.beneficiary;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BeneficiaryResponse(
    Long id,
    String cpf,
    String nome,
    LocalDate dtNascimento,
    String uf,
    BeneficiaryStatus status,
    String codPrograma,
    LocalDateTime criadoEm,
    LocalDateTime atualizadoEm
) {
    public static BeneficiaryResponse from(Beneficiary b) {
        return new BeneficiaryResponse(
            b.getId(), b.getCpf(), b.getNome(), b.getDtNascimento(),
            b.getUf(), b.getStatus(), b.getCodPrograma(),
            b.getCriadoEm(), b.getAtualizadoEm()
        );
    }
}
