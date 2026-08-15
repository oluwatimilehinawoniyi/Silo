package com.silo.loan.service;

import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.entity.GuarantorLiability;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.enums.LiabilityStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.event.LoanDefaultedEvent;
import com.silo.loan.repository.GuarantorLiabilityRepository;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultDetectionService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final LoanGuarantorRepository loanGuarantorRepository;
    private final GuarantorLiabilityRepository guarantorLiabilityRepository;
    private final BorrowerRiskService borrowerRiskService;
    private final GuarantorCredibilityService guarantorCredibilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final int consecutiveLateThreshold;

    public DefaultDetectionService(
            LoanRepository loanRepository,
            LoanInstallmentRepository loanInstallmentRepository,
            LoanGuarantorRepository loanGuarantorRepository,
            GuarantorLiabilityRepository guarantorLiabilityRepository,
            BorrowerRiskService borrowerRiskService,
            GuarantorCredibilityService guarantorCredibilityService,
            ApplicationEventPublisher eventPublisher,
            @Value("${silo.loan.consecutive-late-installments-for-default:3}") int consecutiveLateThreshold) {
        this.loanRepository = loanRepository;
        this.loanInstallmentRepository = loanInstallmentRepository;
        this.loanGuarantorRepository = loanGuarantorRepository;
        this.guarantorLiabilityRepository = guarantorLiabilityRepository;
        this.borrowerRiskService = borrowerRiskService;
        this.guarantorCredibilityService = guarantorCredibilityService;
        this.eventPublisher = eventPublisher;
        this.consecutiveLateThreshold = consecutiveLateThreshold;
    }

    @Transactional
    public int detectDefaults() {
        int defaultedCount = 0;
        for (Loan loan : loanRepository.findByStatus(LoanStatus.ACTIVE)) {
            if (hasConsecutiveLateRun(loan.getId())) {
                processDefault(loan);
                defaultedCount++;
            }
        }
        return defaultedCount;
    }

    private boolean hasConsecutiveLateRun(UUID loanId) {
        int streak = 0;
        for (LoanInstallment installment : loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)) {
            streak = installment.getStatus() == InstallmentStatus.LATE ? streak + 1 : 0;
            if (streak >= consecutiveLateThreshold) {
                return true;
            }
        }
        return false;
    }

    private void processDefault(Loan loan) {
        List<LoanGuarantor> acceptedGuarantors =
                loanGuarantorRepository.findByLoanRequestIdAndStatus(loan.getLoanRequestId(), GuarantorStatus.ACCEPTED);

        List<GuarantorLiability> liabilities = new ArrayList<>();
        if (!acceptedGuarantors.isEmpty()) {
            List<BigDecimal> shares = splitEqually(loan.getOutstandingBalance(), acceptedGuarantors.size());
            for (int i = 0; i < acceptedGuarantors.size(); i++) {
                liabilities.add(GuarantorLiability.builder()
                        .loanId(loan.getId())
                        .guarantorMemberId(acceptedGuarantors.get(i).getMemberId())
                        .amount(shares.get(i))
                        .status(LiabilityStatus.PENDING)
                        .build());
            }
            liabilities = guarantorLiabilityRepository.saveAll(liabilities);
        }

        loan.setStatus(LoanStatus.DEFAULTED);
        loanRepository.save(loan);

        eventPublisher.publishEvent(new LoanDefaultedEvent(
                loan.getId(), loan.getMemberId(), loan.getOutstandingBalance(),
                liabilities.stream().map(GuarantorLiability::getId).toList()));

        borrowerRiskService.recomputeAndSave(loan.getMemberId());
        acceptedGuarantors.forEach(guarantor -> guarantorCredibilityService.recordLoanWentBad(guarantor.getMemberId()));
    }

    private List<BigDecimal> splitEqually(BigDecimal total, int parts) {
        BigDecimal base = total.divide(BigDecimal.valueOf(parts), 2, RoundingMode.DOWN);
        List<BigDecimal> shares = new ArrayList<>();
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (int i = 0; i < parts - 1; i++) {
            shares.add(base);
            runningTotal = runningTotal.add(base);
        }
        shares.add(total.subtract(runningTotal));
        return shares;
    }
}
