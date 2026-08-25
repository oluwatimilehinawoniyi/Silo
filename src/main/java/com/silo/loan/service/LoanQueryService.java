package com.silo.loan.service;

import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.dto.LoanDetailResponse;
import com.silo.loan.dto.LoanInstallmentResponse;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanQueryService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository loanInstallmentRepository;

    public LoanDetailResponse getLoanDetail(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id " + loanId));

        return toDetailResponse(loan);
    }

    public List<LoanDetailResponse> getLoansForMember(UUID memberId) {
        return loanRepository.findByMemberId(memberId).stream()
                .map(this::toDetailResponse)
                .toList();
    }

    private LoanDetailResponse toDetailResponse(Loan loan) {
        var installments = loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId())
                .stream()
                .map(this::toInstallmentResponse)
                .toList();

        return new LoanDetailResponse(
                loan.getId(),
                loan.getLoanRequestId(),
                loan.getMemberId(),
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getDurationMonths(),
                loan.getDisbursedDate(),
                loan.getStatus(),
                loan.getOutstandingBalance(),
                installments);
    }

    private LoanInstallmentResponse toInstallmentResponse(LoanInstallment installment) {
        return new LoanInstallmentResponse(
                installment.getId(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getExpectedAmount(),
                installment.getStatus(),
                installment.getPaidDate());
    }
}
