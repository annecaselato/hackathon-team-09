package com.datacorp.sifap.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** REQ-PAY-001: check for duplicate payment by beneficiary + competence + program */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.cpfBenef = :cpf AND p.anoMesRef = :competence AND (:programa IS NULL OR p.codPrograma = :programa)")
    boolean existsByCpfAndCompetenceAndProgram(
        @Param("cpf") String cpf,
        @Param("competence") String competence,
        @Param("programa") String programa);

    Page<Payment> findByCpfBenef(String cpf, Pageable pageable);

    Page<Payment> findByAnoMesRef(String anoMesRef, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    Optional<Payment> findById(Long id);
}
