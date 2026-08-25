package com.silo.loan.repository;

import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.enums.GuarantorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanGuarantorRepository extends JpaRepository<LoanGuarantor, UUID> {

    boolean existsByLoanRequestIdAndMemberId(UUID loanRequestId, UUID memberId);

    List<LoanGuarantor> findByLoanRequestId(UUID loanRequestId);

    List<LoanGuarantor> findByLoanRequestIdAndStatus(UUID loanRequestId, GuarantorStatus status);

    long countByMemberIdAndStatus(UUID memberId, GuarantorStatus status);

    List<LoanGuarantor> findByMemberIdAndStatus(UUID memberId, GuarantorStatus status);
}
