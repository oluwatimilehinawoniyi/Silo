package com.silo.loan.repository;

import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.enums.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, UUID> {

    List<LoanInstallment> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    boolean existsByLoanId(UUID loanId);

    long countByLoanIdInAndStatus(List<UUID> loanIds, InstallmentStatus status);

    List<LoanInstallment> findByStatusAndDueDateBefore(InstallmentStatus status, LocalDateTime cutoff);
}
