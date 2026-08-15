package com.silo.loan.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.dto.LoanApprovalRequest;
import com.silo.loan.dto.LoanRequestResponse;
import com.silo.loan.dto.LoanResponse;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.LoanRequestStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.event.LoanApprovedEvent;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanRepository;
import com.silo.loan.repository.LoanRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanApprovalService {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanRepository loanRepository;
    private final LoanGuarantorRepository loanGuarantorRepository;
    private final LoanInstallmentService loanInstallmentService;
    private final BorrowerRiskService borrowerRiskService;
    private final GuarantorCredibilityService guarantorCredibilityService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoanResponse approve(UUID loanRequestId, LoanApprovalRequest request) {
        LoanRequest loanRequest = requirePendingRequest(loanRequestId);

        List<LoanGuarantor> acceptedGuarantors =
                loanGuarantorRepository.findByLoanRequestIdAndStatus(loanRequestId, GuarantorStatus.ACCEPTED);
        if (acceptedGuarantors.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "At least one ACCEPTED guarantor is required before approval");
        }

        if (borrowerRiskService.hasActiveDefault(loanRequest.getMemberId())) {
            throw new BusinessRuleViolationException(
                    "Borrower has an active default and cannot be approved for a new loan");
        }

        if (loanRepository.existsByMemberIdAndStatus(loanRequest.getMemberId(), LoanStatus.ACTIVE)) {
            throw new BusinessRuleViolationException(
                    "Borrower already has an active loan and cannot be approved for another");
        }

        for (LoanGuarantor guarantor : acceptedGuarantors) {
            if (!guarantorCredibilityService.meetsMinimumCredibility(guarantor.getMemberId())) {
                throw new BusinessRuleViolationException(
                        "Guarantor " + guarantor.getMemberId() + " does not meet the minimum credibility requirement");
            }
        }

        Loan loan = Loan.builder()
                .loanRequestId(loanRequestId)
                .memberId(loanRequest.getMemberId())
                .principalAmount(loanRequest.getAmountRequested())
                .interestRate(request.interestRate())
                .durationMonths(request.durationMonths())
                .disbursedDate(LocalDateTime.now())
                .status(LoanStatus.ACTIVE)
                .outstandingBalance(loanRequest.getAmountRequested())
                .build();
        loan = loanRepository.save(loan);

        loanInstallmentService.generateSchedule(loan);

        loanRequest.setStatus(LoanRequestStatus.APPROVED);
        loanRequestRepository.save(loanRequest);

        eventPublisher.publishEvent(new LoanApprovedEvent(
                loan.getId(), loanRequestId, loan.getMemberId(), loan.getPrincipalAmount(), loan.getDisbursedDate()));

        return toLoanResponse(loan);
    }

    @Transactional
    public LoanRequestResponse reject(UUID loanRequestId) {
        LoanRequest loanRequest = requirePendingRequest(loanRequestId);

        loanRequest.setStatus(LoanRequestStatus.REJECTED);
        loanRequest = loanRequestRepository.save(loanRequest);

        return new LoanRequestResponse(
                loanRequest.getId(),
                loanRequest.getMemberId(),
                loanRequest.getAmountRequested(),
                loanRequest.getPurpose(),
                loanRequest.getStatus(),
                loanRequest.getSubmittedAt());
    }

    private LoanRequest requirePendingRequest(UUID loanRequestId) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan request not found with id " + loanRequestId));

        if (loanRequest.getStatus() != LoanRequestStatus.PENDING) {
            throw new BusinessRuleViolationException("This loan request has already been decided");
        }

        return loanRequest;
    }

    private LoanResponse toLoanResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getLoanRequestId(),
                loan.getMemberId(),
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getDurationMonths(),
                loan.getDisbursedDate(),
                loan.getStatus(),
                loan.getOutstandingBalance());
    }
}
