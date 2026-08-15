package com.silo.loan.service;

import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.repository.LoanInstallmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class InstallmentDueDateSweepService {

    private final LoanInstallmentRepository loanInstallmentRepository;
    private final BorrowerRiskService borrowerRiskService;
    private final int gracePeriodDays;

    public InstallmentDueDateSweepService(
            LoanInstallmentRepository loanInstallmentRepository,
            BorrowerRiskService borrowerRiskService,
            @Value("${silo.loan.installment.grace-period-days:3}") int gracePeriodDays) {
        this.loanInstallmentRepository = loanInstallmentRepository;
        this.borrowerRiskService = borrowerRiskService;
        this.gracePeriodDays = gracePeriodDays;
    }

    @Transactional
    public int sweepOverdueInstallments() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(gracePeriodDays);
        List<LoanInstallment> overdue = loanInstallmentRepository.findByStatusAndDueDateBefore(
                InstallmentStatus.PENDING, cutoff);

        Set<UUID> affectedMemberIds = new HashSet<>();
        for (LoanInstallment installment : overdue) {
            installment.setStatus(InstallmentStatus.LATE);
            affectedMemberIds.add(installment.getLoan().getMemberId());
        }
        loanInstallmentRepository.saveAll(overdue);

        affectedMemberIds.forEach(borrowerRiskService::recomputeAndSave);

        return overdue.size();
    }
}
