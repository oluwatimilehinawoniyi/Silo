package com.silo.repayment.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.GuarantorLiabilityLookup;
import com.silo.loan.LoanLookup;
import com.silo.repayment.dto.LiabilityRepaymentRequest;
import com.silo.repayment.dto.RepaymentRequest;
import com.silo.repayment.dto.RepaymentResponse;
import com.silo.repayment.entity.Repayment;
import com.silo.repayment.event.RepaymentMadeEvent;
import com.silo.repayment.repository.RepaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final LoanLookup loanLookup;
    private final GuarantorLiabilityLookup guarantorLiabilityLookup;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public RepaymentResponse recordBorrowerRepayment(UUID payerMemberId, RepaymentRequest request) {
        if (!loanLookup.exists(request.loanId())) {
            throw new ResourceNotFoundException("Loan not found with id " + request.loanId());
        }
        if (!loanLookup.isActive(request.loanId())) {
            throw new BusinessRuleViolationException("Repayments can only be recorded against an ACTIVE loan");
        }
        if (!loanLookup.belongsToMember(request.loanId(), payerMemberId)) {
            throw new AccessDeniedException("You are not the borrower on this loan");
        }

        Repayment repayment = Repayment.builder()
                .loanId(request.loanId())
                .payerMemberId(payerMemberId)
                .liabilityId(null)
                .amount(request.amount())
                .reference(request.reference())
                .build();

        repayment = repaymentRepository.save(repayment);

        eventPublisher.publishEvent(new RepaymentMadeEvent(
                repayment.getId(), repayment.getLoanId(), repayment.getPayerMemberId(),
                repayment.getAmount(), repayment.getLiabilityId()));

        return toResponse(repayment);
    }

    @Transactional
    public RepaymentResponse recordLiabilityRepayment(UUID payerMemberId, LiabilityRepaymentRequest request) {
        if (!guarantorLiabilityLookup.exists(request.liabilityId())) {
            throw new ResourceNotFoundException("Guarantor liability not found with id " + request.liabilityId());
        }
        if (!guarantorLiabilityLookup.isPending(request.liabilityId())) {
            throw new BusinessRuleViolationException("This guarantor liability is not PENDING");
        }
        if (!guarantorLiabilityLookup.belongsToGuarantor(request.liabilityId(), payerMemberId)) {
            throw new AccessDeniedException("You are not the guarantor assigned to this liability");
        }

        UUID loanId = guarantorLiabilityLookup.findLoanId(request.liabilityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Guarantor liability not found with id " + request.liabilityId()));

        Repayment repayment = Repayment.builder()
                .loanId(loanId)
                .payerMemberId(payerMemberId)
                .liabilityId(request.liabilityId())
                .amount(request.amount())
                .reference(request.reference())
                .build();

        repayment = repaymentRepository.save(repayment);

        eventPublisher.publishEvent(new RepaymentMadeEvent(
                repayment.getId(), repayment.getLoanId(), repayment.getPayerMemberId(),
                repayment.getAmount(), repayment.getLiabilityId()));

        return toResponse(repayment);
    }

    public List<RepaymentResponse> getHistory(UUID loanId) {
        return repaymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RepaymentResponse toResponse(Repayment repayment) {
        return new RepaymentResponse(
                repayment.getId(),
                repayment.getLoanId(),
                repayment.getPayerMemberId(),
                repayment.getLiabilityId(),
                repayment.getAmount(),
                repayment.getReference(),
                repayment.getPaymentDate());
    }
}
