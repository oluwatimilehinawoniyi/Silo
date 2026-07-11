package com.silo.loan.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanInstallmentService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private final LoanInstallmentRepository loanInstallmentRepository;
    private final LoanRepository loanRepository;

    @Transactional
    public List<LoanInstallment> generateSchedule(Loan loan) {
        if (loan.getDisbursedDate() == null) {
            throw new BusinessRuleViolationException(
                    "Cannot generate a repayment schedule before the loan is disbursed");
        }

        if (loanInstallmentRepository.existsByLoanId(loan.getId())) {
            throw new BusinessRuleViolationException(
                    "Repayment schedule already generated for loan " + loan.getId());
        }

        List<BigDecimal> installmentAmounts = calculateInstallmentAmounts(
                loan.getPrincipalAmount(), loan.getInterestRate(), loan.getDurationMonths());

        List<LoanInstallment> installments = new ArrayList<>();
        for (int i = 0; i < installmentAmounts.size(); i++) {
            installments.add(LoanInstallment.builder()
                    .loan(loan)
                    .installmentNumber(i + 1)
                    .dueDate(loan.getDisbursedDate().plusMonths(i + 1))
                    .expectedAmount(installmentAmounts.get(i))
                    .status(InstallmentStatus.PENDING)
                    .build());
        }

        BigDecimal totalRepayable = installmentAmounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        loan.setOutstandingBalance(totalRepayable);
        loanRepository.save(loan);

        return loanInstallmentRepository.saveAll(installments);
    }

    private List<BigDecimal> calculateInstallmentAmounts(
            BigDecimal principal, BigDecimal annualRatePercent, int durationMonths) {
        BigDecimal totalInterest = principal
                .multiply(annualRatePercent)
                .multiply(BigDecimal.valueOf(durationMonths))
                .divide(HUNDRED.multiply(TWELVE), 10, RoundingMode.HALF_UP);

        BigDecimal totalRepayable = principal.add(totalInterest);

        BigDecimal baseInstallment = totalRepayable
                .divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.DOWN);

        List<BigDecimal> amounts = new ArrayList<>();
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (int i = 0; i < durationMonths - 1; i++) {
            amounts.add(baseInstallment);
            runningTotal = runningTotal.add(baseInstallment);
        }
        amounts.add(totalRepayable.subtract(runningTotal));

        return amounts;
    }
}
