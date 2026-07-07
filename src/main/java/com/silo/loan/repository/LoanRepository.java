package com.silo.loan.repository;

import com.silo.loan.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    boolean existsByLoanRequestId(UUID loanRequestId);
}
