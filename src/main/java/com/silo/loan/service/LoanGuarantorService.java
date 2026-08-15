package com.silo.loan.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.DuplicateResourceException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.dto.AddGuarantorRequest;
import com.silo.loan.dto.LoanGuarantorResponse;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.LoanRequestStatus;
import com.silo.loan.event.GuarantorInvitedEvent;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanRequestRepository;
import com.silo.member.MemberLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanGuarantorService {

    private final LoanGuarantorRepository loanGuarantorRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final MemberLookup memberLookup;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoanGuarantorResponse addGuarantor(UUID borrowerMemberId, UUID loanRequestId, AddGuarantorRequest request) {
        LoanRequest loanRequest = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan request not found with id " + loanRequestId));

        if (!loanRequest.getMemberId().equals(borrowerMemberId)) {
            throw new AccessDeniedException("Only the requesting member can add guarantors to this loan request");
        }
        if (loanRequest.getStatus() != LoanRequestStatus.PENDING) {
            throw new BusinessRuleViolationException("Cannot add a guarantor to a request that is not PENDING");
        }

        UUID guarantorMemberId = request.guarantorMemberId();
        if (guarantorMemberId.equals(borrowerMemberId)) {
            throw new BusinessRuleViolationException("A member cannot guarantee their own loan request");
        }
        if (!memberLookup.exists(guarantorMemberId)) {
            throw new ResourceNotFoundException("Member not found with id " + guarantorMemberId);
        }
        if (!memberLookup.isActiveAndVerified(guarantorMemberId)) {
            throw new BusinessRuleViolationException("Guarantor must be ACTIVE and KYC_VERIFIED");
        }
        if (loanGuarantorRepository.existsByLoanRequestIdAndMemberId(loanRequestId, guarantorMemberId)) {
            throw new DuplicateResourceException("This member has already been invited to guarantee this request");
        }

        LoanGuarantor guarantor = LoanGuarantor.builder()
                .loanRequestId(loanRequestId)
                .memberId(guarantorMemberId)
                .status(GuarantorStatus.PENDING)
                .build();

        guarantor = loanGuarantorRepository.save(guarantor);

        eventPublisher.publishEvent(new GuarantorInvitedEvent(guarantor.getId(), loanRequestId, guarantorMemberId));

        return toResponse(guarantor);
    }

    @Transactional
    public LoanGuarantorResponse accept(UUID guarantorMemberId, UUID loanGuarantorId) {
        return respond(guarantorMemberId, loanGuarantorId, GuarantorStatus.ACCEPTED);
    }

    @Transactional
    public LoanGuarantorResponse decline(UUID guarantorMemberId, UUID loanGuarantorId) {
        return respond(guarantorMemberId, loanGuarantorId, GuarantorStatus.DECLINED);
    }

    private LoanGuarantorResponse respond(UUID guarantorMemberId, UUID loanGuarantorId, GuarantorStatus decision) {
        LoanGuarantor guarantor = loanGuarantorRepository.findById(loanGuarantorId)
                .orElseThrow(() -> new ResourceNotFoundException("Guarantor invite not found with id " + loanGuarantorId));

        if (!guarantor.getMemberId().equals(guarantorMemberId)) {
            throw new AccessDeniedException("You are not the invited guarantor for this invite");
        }
        if (guarantor.getStatus() != GuarantorStatus.PENDING) {
            throw new BusinessRuleViolationException("This guarantor invite has already been responded to");
        }

        guarantor.setStatus(decision);
        guarantor = loanGuarantorRepository.save(guarantor);

        return toResponse(guarantor);
    }

    private LoanGuarantorResponse toResponse(LoanGuarantor guarantor) {
        return new LoanGuarantorResponse(
                guarantor.getId(),
                guarantor.getLoanRequestId(),
                guarantor.getMemberId(),
                guarantor.getStatus(),
                guarantor.getInvitedAt());
    }
}
