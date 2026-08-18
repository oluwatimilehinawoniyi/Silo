package com.silo.contribution.repository;

import com.silo.contribution.AutoDebitMandateStatus;
import com.silo.contribution.entity.ContributionAutoDebitMandate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContributionAutoDebitMandateRepository extends JpaRepository<ContributionAutoDebitMandate, UUID> {

    Optional<ContributionAutoDebitMandate> findByMemberIdAndStatusIn(UUID memberId, List<AutoDebitMandateStatus> statuses);

    List<ContributionAutoDebitMandate> findByStatusAndNextChargeDateLessThanEqual(
            AutoDebitMandateStatus status, LocalDate date);
}
