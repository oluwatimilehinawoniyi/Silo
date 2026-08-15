package com.silo.reporting.repository;

import com.silo.reporting.entity.ReportingMemberSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReportingMemberSummaryRepository extends JpaRepository<ReportingMemberSummary, UUID> {

    List<ReportingMemberSummary> findAllByOrderByTotalContributionsDesc(Pageable pageable);

    @Query("select coalesce(sum(m.totalContributions), 0) from ReportingMemberSummary m")
    BigDecimal sumTotalContributions();
}
