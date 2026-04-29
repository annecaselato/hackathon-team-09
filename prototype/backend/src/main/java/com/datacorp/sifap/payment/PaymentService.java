package com.datacorp.sifap.payment;

import com.datacorp.sifap.audit.AuditAction;
import com.datacorp.sifap.audit.AuditService;
import com.datacorp.sifap.beneficiary.Beneficiary;
import com.datacorp.sifap.beneficiary.BeneficiaryRepository;
import com.datacorp.sifap.calculation.BenefitCalculationService;
import com.datacorp.sifap.calculation.DiscountCeilingPolicy;
import com.datacorp.sifap.shared.exception.BusinessException;
import com.datacorp.sifap.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payment business logic.
 * Implements: REQ-PAY-001 (no duplicates), REQ-PAY-002 (status domain),
 *             REQ-PAY-003 (30% discount ceiling), REQ-PAY-004 (corrections).
 */
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final DiscountCeilingPolicy ceilingPolicy;
    private final BenefitCalculationService calculationService;
    private final AuditService auditService;

    public PaymentService(
        PaymentRepository paymentRepository,
        BeneficiaryRepository beneficiaryRepository,
        DiscountCeilingPolicy ceilingPolicy,
        BenefitCalculationService calculationService,
        AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.ceilingPolicy = ceilingPolicy;
        this.calculationService = calculationService;
        this.auditService = auditService;
    }

    /**
     * Creates a payment for a beneficiary in a given competence.
     * REQ-PAY-001: rejects if payment already exists for (CPF, AAAAMM, programa).
     */
    public Payment create(PaymentRequest request, String userId) {
        // REQ-PAY-001: prevent duplicate
        if (paymentRepository.existsByCpfAndCompetenceAndProgram(
            request.cpfBenef(), request.anoMesRef(), request.codPrograma())) {
            throw new BusinessException(
                "Payment already exists for CPF " + request.cpfBenef() +
                " / competence " + request.anoMesRef());
        }

        Beneficiary beneficiary = beneficiaryRepository.findByCpf(request.cpfBenef())
            .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found: " + request.cpfBenef()));

        Payment payment = new Payment();
        payment.setBeneficiary(beneficiary);
        payment.setCpfBenef(request.cpfBenef());
        payment.setAnoMesRef(request.anoMesRef());
        payment.setCodPrograma(request.codPrograma());
        payment.setVlrBruto(request.vlrBruto());
        payment.setStatus(PaymentStatus.P);
        payment.setUsuarioGeracao(userId);

        // Apply discounts with ceiling
        if (request.descontos() != null && !request.descontos().isEmpty()) {
            List<Discount> discounts = request.descontos().stream()
                .map(d -> new Discount(payment, d.tipo(), d.valor(), d.descricao()))
                .toList();
            payment.getDescontos().addAll(discounts);

            BigDecimal totalDiscount = ceilingPolicy.apply(request.vlrBruto(), discounts);
            payment.setVlrDescontoTotal(totalDiscount);
        }

        payment.setVlrLiquido(payment.getVlrBruto().subtract(payment.getVlrDescontoTotal()));

        Payment saved = paymentRepository.save(payment);

        auditService.record(AuditAction.IN, "PGTO", String.valueOf(saved.getId()),
            null, saved, userId, "PaymentService");

        return saved;
    }

    @Transactional(readOnly = true)
    public Payment findById(Long id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Payment> list(String cpf, String anoMesRef, PaymentStatus status, Pageable pageable) {
        if (cpf != null) return paymentRepository.findByCpfBenef(cpf, pageable);
        if (anoMesRef != null) return paymentRepository.findByAnoMesRef(anoMesRef, pageable);
        if (status != null) return paymentRepository.findByStatus(status, pageable);
        return paymentRepository.findAll(pageable);
    }

    /**
     * REQ-PAY-004: update existing payment record with audit trail.
     */
    public Payment correct(Long id, PaymentCorrectionRequest request, String userId) {
        Payment payment = findById(id);
        Payment before = snapshotPayment(payment);

        if (request.vlrBruto() != null) {
            payment.setVlrBruto(request.vlrBruto());
        }
        if (request.status() != null) {
            payment.setStatus(request.status());
        }

        // Recalculate net if gross changed
        payment.setVlrLiquido(payment.getVlrBruto().subtract(payment.getVlrDescontoTotal()));

        Payment saved = paymentRepository.save(payment);

        auditService.record(AuditAction.AL, "PGTO", String.valueOf(id),
            before, saved, userId, "PaymentService");

        return saved;
    }

    private Payment snapshotPayment(Payment source) {
        Payment copy = new Payment();
        copy.setCpfBenef(source.getCpfBenef());
        copy.setAnoMesRef(source.getAnoMesRef());
        copy.setVlrBruto(source.getVlrBruto());
        copy.setVlrDescontoTotal(source.getVlrDescontoTotal());
        copy.setVlrLiquido(source.getVlrLiquido());
        copy.setStatus(source.getStatus());
        return copy;
    }
}
