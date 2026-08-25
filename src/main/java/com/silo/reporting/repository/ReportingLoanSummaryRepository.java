package com.silo.reporting.repository;

import com.silo.reporting.entity.ReportingLoanSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface ReportingLoanSummaryRepository extends JpaRepository<ReportingLoanSummary, UUID> {

    long countByStatus(String status);

    @Query("select coalesce(sum(l.outstandingBalance), 0) from ReportingLoanSummary l where l.status = :status")
    BigDecimal sumOutstandingBalanceByStatus(@Param("status") String status);

    @Query("select coalesce(sum(l.outstandingBalance), 0) from ReportingLoanSummary l "
            + "where l.memberId = :memberId and l.status = :status")
    BigDecimal sumOutstandingBalanceByMemberIdAndStatus(
            @Param("memberId") UUID memberId, @Param("status") String status);
}
