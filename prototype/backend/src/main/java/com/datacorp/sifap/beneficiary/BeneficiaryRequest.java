package com.datacorp.sifap.beneficiary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for creating/updating a beneficiary.
 * Validation annotations are the HTTP boundary guard — service also validates business rules.
 */
public record BeneficiaryRequest(
    @NotBlank @Size(min = 11, max = 14) String cpf,
    @NotBlank @Size(min = 3, max = 100) String nome,
    @NotNull LocalDate dtNascimento,
    @NotBlank @Size(min = 2, max = 2) String uf,
    BeneficiaryStatus status,
    @Size(max = 3) String codPrograma
) {}
