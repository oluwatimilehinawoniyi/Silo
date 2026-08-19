package com.silo.contribution.repository;

import com.silo.contribution.entity.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ContributionRepository extends JpaRepository<Contribution, UUID> {

    List<Contribution> findByMemberIdOrderByContributionDateDesc(
            UUID memberId);

    List<Contribution> findAllByOrderByContributionDateDesc();

    @Query("select coalesce(sum(c.amount), 0) from Contribution c where c.memberId = :memberId")
    BigDecimal sumAmountByMemberId(@Param("memberId") UUID memberId);

    long countByMemberId(UUID memberId);
}
