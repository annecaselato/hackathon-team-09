package com.datacorp.sifap.beneficiary;

import com.datacorp.sifap.audit.AuditService;
import com.datacorp.sifap.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BeneficiaryService.
 * Covers: REQ-BEN-001 (CPF), REQ-BEN-002 (name), REQ-BEN-003 (birth date),
 *         REQ-BEN-004 (UF), REQ-BEN-005 (status).
 */
@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository repository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private BeneficiaryService service;

    private BeneficiaryRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new BeneficiaryRequest(
            "11144477735",       // valid CPF (test vector)
            "João Silva",
            LocalDate.of(1980, 6, 15),
            "SP",
            BeneficiaryStatus.A,
            "A"
        );
        when(repository.existsByCpf(anyString())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).record(any(), any(), any(), any(), any(), any(), any());
    }

    // ---- REQ-BEN-001: CPF validation ----

    @Test
    void givenValidCpf_whenRegister_thenAccepts() {
        assertThatCode(() -> service.register(validRequest, "OPJOSE")).doesNotThrowAnyException();
    }

    @Test
    void givenInvalidCpf_whenRegister_thenRejectsWithMessage() {
        var req = new BeneficiaryRequest("11144477736", "João Silva",
            LocalDate.of(1980, 1, 1), "SP", null, null);
        assertThatThrownBy(() -> service.register(req, "OPJOSE"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("modulo-11");
    }

    @Test
    void givenAllZerosCpf_whenRegister_thenRejects() {
        var req = new BeneficiaryRequest("00000000000", "João Silva",
            LocalDate.of(1980, 1, 1), "SP", null, null);
        assertThatThrownBy(() -> service.register(req, "OPJOSE"))
            .isInstanceOf(BusinessException.class);
    }

    // ---- REQ-BEN-002: Name validation ----

    @Test
    void givenSingleWordName_whenRegister_thenRejects() {
        var req = new BeneficiaryRequest("11144477735", "João",
            LocalDate.of(1980, 1, 1), "SP", null, null);
        assertThatThrownBy(() -> service.register(req, "OPJOSE"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("surname");
    }

    @Test
    void givenBlankName_whenRegister_thenRejects() {
        var req = new BeneficiaryRequest("11144477735", "  ",
            LocalDate.of(1980, 1, 1), "SP", null, null);
        assertThatThrownBy(() -> service.register(req, "OPJOSE"))
            .isInstanceOf(BusinessException.class);
    }

    // ---- REQ-BEN-003: Birth date validation ----

    @Test
    void givenBirthDateBefore1900_whenRegister_thenRejects() {
        var req = new BeneficiaryRequest("11144477735", "João Silva",
            LocalDate.of(1899, 12, 31), "SP", null, null);
        assertThatThrownBy(() -> service.register(req, "OPJOSE"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("1900");
    }

    @Test
    void givenFutureBirthDate_whenRegister_thenRejects() {
        var req = new BeneficiaryRequest("11144477735", "João Silva",
            LocalDate.now().plusDays(1), "SP", null, null);
        assertThatThrownBy(() -> service.register(req, "OPJOSE"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("future");
    }

    // ---- REQ-BEN-004: UF validation ----

    @Test
    void givenInvalidUf_whenRegister_thenRejects() {
        var req = new BeneficiaryRequest("11144477735", "João Silva",
            LocalDate.of(1980, 1, 1), "XY", null, null);
        assertThatThrownBy(() -> service.register(req, "OPJOSE"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Invalid state code");
    }

    @Test
    void givenLowercaseUf_whenRegister_thenNormalizesAndAccepts() {
        var req = new BeneficiaryRequest("11144477735", "João Silva",
            LocalDate.of(1980, 1, 1), "sp", null, null);
        assertThatCode(() -> service.register(req, "OPJOSE")).doesNotThrowAnyException();
    }

    // ---- Duplicate CPF ----

    @Test
    void givenDuplicateCpf_whenRegister_thenRejects() {
        when(repository.existsByCpf(anyString())).thenReturn(true);
        assertThatThrownBy(() -> service.register(validRequest, "OPJOSE"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("already registered");
    }

    // ---- Default status ----

    @Test
    void givenNullStatus_whenRegister_thenDefaultsToActive() {
        var req = new BeneficiaryRequest("11144477735", "João Silva",
            LocalDate.of(1980, 1, 1), "SP", null, null);
        when(repository.save(any())).thenAnswer(inv -> {
            Beneficiary b = inv.getArgument(0);
            assertThat(b.getStatus()).isEqualTo(BeneficiaryStatus.A);
            return b;
        });
        service.register(req, "OPJOSE");
    }

    // ---- findById ----

    @Test
    void givenExistingId_whenFindById_thenReturns() {
        Beneficiary b = new Beneficiary();
        b.setCpf("11144477735");
        when(repository.findById(1L)).thenReturn(Optional.of(b));
        assertThat(service.findById(1L)).isNotNull();
    }
}
