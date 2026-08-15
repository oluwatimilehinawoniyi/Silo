package com.silo.loan.repository;

import com.silo.loan.entity.Loan;
import com.silo.loan.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    boolean existsByLoanRequestId(UUID loanRequestId);

    long countByMemberId(UUID memberId);

    long countByMemberIdAndStatus(UUID memberId, LoanStatus status);

    boolean existsByMemberIdAndStatus(UUID memberId, LoanStatus status);

    List<UUID> findIdByMemberId(UUID memberId);
}
