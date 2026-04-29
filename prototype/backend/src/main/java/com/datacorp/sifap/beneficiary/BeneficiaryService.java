package com.datacorp.sifap.beneficiary;

import com.datacorp.sifap.audit.AuditAction;
import com.datacorp.sifap.audit.AuditService;
import com.datacorp.sifap.shared.exception.BusinessException;
import com.datacorp.sifap.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * Beneficiary business logic.
 * Implements: REQ-BEN-001 (CPF modulo-11), REQ-BEN-002 (name surname),
 *             REQ-BEN-003 (birth date), REQ-BEN-004 (UF), REQ-BEN-005 (status domain).
 */
@Service
@Transactional
public class BeneficiaryService {

    private static final Set<String> VALID_UF_CODES = Set.of(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
        "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
        "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private final BeneficiaryRepository repository;
    private final AuditService auditService;

    public BeneficiaryService(BeneficiaryRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /** REQ-BEN-001 to REQ-BEN-005: full validation + create */
    public Beneficiary register(BeneficiaryRequest request, String userId) {
        validateCpf(request.cpf());
        validateName(request.nome());
        validateBirthDate(request.dtNascimento());
        validateUf(request.uf());

        String normalizedCpf = CpfValidator.normalize(request.cpf());

        if (repository.existsByCpf(normalizedCpf)) {
            throw new BusinessException("CPF already registered: " + normalizedCpf);
        }

        Beneficiary entity = new Beneficiary();
        entity.setCpf(normalizedCpf);
        entity.setNome(request.nome().trim());
        entity.setDtNascimento(request.dtNascimento());
        entity.setUf(request.uf().toUpperCase());
        entity.setStatus(request.status() != null ? request.status() : BeneficiaryStatus.A);
        entity.setCodPrograma(request.codPrograma());
        entity.setUsuarioCriacao(userId);

        Beneficiary saved = repository.save(entity);

        auditService.record(AuditAction.IN, "BENF", String.valueOf(saved.getId()),
            null, saved, userId, "BeneficiaryService");

        return saved;
    }

    @Transactional(readOnly = true)
    public Beneficiary findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found: " + id));
    }

    @Transactional(readOnly = true)
    public Beneficiary findByCpf(String cpf) {
        String normalized = CpfValidator.normalize(cpf);
        return repository.findByCpf(normalized)
            .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found with CPF: " + cpf));
    }

    @Transactional(readOnly = true)
    public Page<Beneficiary> list(BeneficiaryStatus status, String nome, Pageable pageable) {
        if (status != null) return repository.findByStatus(status, pageable);
        if (nome != null && !nome.isBlank()) return repository.findByNomeContainingIgnoreCase(nome, pageable);
        return repository.findAll(pageable);
    }

    public Beneficiary updateStatus(Long id, BeneficiaryStatus newStatus, String userId) {
        Beneficiary entity = findById(id);
        Beneficiary before = snapshot(entity);

        entity.setStatus(newStatus);
        Beneficiary saved = repository.save(entity);

        auditService.record(AuditAction.AL, "BENF", String.valueOf(id),
            before, saved, userId, "BeneficiaryService");

        return saved;
    }

    // --- Validation helpers (REQ-BEN-001 to 005) ---

    private void validateCpf(String cpf) {
        if (!CpfValidator.isValid(cpf)) {
            throw new BusinessException("Invalid CPF: failed modulo-11 validation");
        }
    }

    private void validateName(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException("Name is required");
        }
        String trimmed = nome.trim();
        if (!trimmed.contains(" ")) {
            throw new BusinessException("Name must contain surname (space-separated)");
        }
    }

    private void validateBirthDate(LocalDate date) {
        if (date == null) throw new BusinessException("Birth date is required");
        if (date.getYear() < 1900) {
            throw new BusinessException("Birth year must be 1900 or later");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException("Birth date cannot be in the future");
        }
    }

    private void validateUf(String uf) {
        if (uf == null || uf.isBlank()) throw new BusinessException("State (UF) is required");
        if (!VALID_UF_CODES.contains(uf.toUpperCase())) {
            throw new BusinessException("Invalid state code. Valid values: " +
                String.join(", ", VALID_UF_CODES.stream().sorted().toList()));
        }
    }

    private Beneficiary snapshot(Beneficiary source) {
        Beneficiary copy = new Beneficiary();
        copy.setCpf(source.getCpf());
        copy.setNome(source.getNome());
        copy.setDtNascimento(source.getDtNascimento());
        copy.setUf(source.getUf());
        copy.setStatus(source.getStatus());
        copy.setCodPrograma(source.getCodPrograma());
        return copy;
    }
}
