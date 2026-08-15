package com.silo.loan.repository;

import com.silo.loan.entity.GuarantorLiability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GuarantorLiabilityRepository extends JpaRepository<GuarantorLiability, UUID> {

    List<GuarantorLiability> findByLoanId(UUID loanId);
}
