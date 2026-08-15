package com.silo.repayment.repository;

import com.silo.repayment.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepaymentRepository extends JpaRepository<Repayment, UUID> {

    List<Repayment> findByLoanIdOrderByPaymentDateDesc(UUID loanId);
}
