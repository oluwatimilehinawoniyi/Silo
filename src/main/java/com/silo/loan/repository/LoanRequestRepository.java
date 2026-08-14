package com.silo.loan.repository;

import com.silo.loan.entity.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, UUID> {

    List<LoanRequest> findByMemberIdOrderBySubmittedAtDesc(UUID memberId);
}
