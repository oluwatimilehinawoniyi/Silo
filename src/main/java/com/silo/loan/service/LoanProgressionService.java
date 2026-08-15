package com.silo.loan.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.LoanProgressionRecorder;
import com.silo.loan.entity.GuarantorLiability;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.enums.LiabilityStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.repository.GuarantorLiabilityRepository;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class LoanProgressionService implements LoanProgressionRecorder {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;
    private final GuarantorLiabilityRepository guarantorLiabilityRepository;
    private final LoanGuarantorRepository loanGuarantorRepository;
    private final GuarantorCredibilityService guarantorCredibilityService;

    @Override
    @Transactional
    public void applyRepayment(UUID loanId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id " + loanId));

        if (amount.compareTo(loan.getOutstandingBalance()) > 0) {
            throw new BusinessRuleViolationException("Repayment amount exceeds the loan's outstanding balance");
        }

        settle(loan, amount);
    }

    @Override
    @Transactional
    public void applyLiabilityRepayment(UUID liabilityId, BigDecimal amount) {
        GuarantorLiability liability = guarantorLiabilityRepository.findById(liabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Guarantor liability not found with id " + liabilityId));

        BigDecimal remainingOwed = liability.getAmount().subtract(liability.getAmountPaid());
        if (amount.compareTo(remainingOwed) > 0) {
            throw new BusinessRuleViolationException("Repayment amount exceeds the liability's remaining balance");
        }

        liability.setAmountPaid(liability.getAmountPaid().add(amount));
        if (liability.getAmountPaid().compareTo(liability.getAmount()) >= 0) {
            liability.setStatus(LiabilityStatus.PAID);
        }
        guarantorLiabilityRepository.save(liability);

        Loan loan = loanRepository.findById(liability.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id " + liability.getLoanId()));
        settle(loan, amount);
    }

    private void settle(Loan loan, BigDecimal amount) {
        loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(amount));
        allocateToInstallments(loan.getId(), amount);

        LoanStatus previousStatus = loan.getStatus();
        if (loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(LoanStatus.CLOSED);
        }
        loanRepository.save(loan);

        if (loan.getStatus() == LoanStatus.CLOSED && previousStatus == LoanStatus.ACTIVE) {
            rewardGuarantors(loan);
        }
    }

    private void allocateToInstallments(UUID loanId, BigDecimal amount) {
        BigDecimal remaining = amount;
        for (LoanInstallment installment : loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (installment.getStatus() == InstallmentStatus.PAID) {
                continue;
            }

            BigDecimal due = installment.getExpectedAmount().subtract(installment.getAmountPaid());
            BigDecimal applied = remaining.min(due);
            installment.setAmountPaid(installment.getAmountPaid().add(applied));
            remaining = remaining.subtract(applied);

            if (installment.getAmountPaid().compareTo(installment.getExpectedAmount()) >= 0) {
                installment.setStatus(InstallmentStatus.PAID);
                installment.setPaidDate(LocalDateTime.now());
            }
            loanInstallmentRepository.save(installment);
        }
    }

    private void rewardGuarantors(Loan loan) {
        List<LoanGuarantor> acceptedGuarantors =
                loanGuarantorRepository.findByLoanRequestIdAndStatus(loan.getLoanRequestId(), GuarantorStatus.ACCEPTED);
        acceptedGuarantors.forEach(guarantor -> guarantorCredibilityService.recordSuccessfulGuarantee(guarantor.getMemberId()));
    }
}
