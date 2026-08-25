package com.silo.loan.repository;

import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.LoanRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, UUID> {

    List<LoanRequest> findByMemberIdOrderBySubmittedAtDesc(UUID memberId);

    @Query("select lr from LoanRequest lr where "
            + "(:memberId is null or lr.memberId = :memberId) and "
            + "(:status is null or lr.status = :status) "
            + "order by lr.submittedAt desc")
    List<LoanRequest> search(@Param("memberId") UUID memberId, @Param("status") LoanRequestStatus status);
}
