package com.silo.loan.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.dto.LoanRequestResponse;
import com.silo.loan.dto.LoanRequestSubmitRequest;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.LoanRequestStatus;
import com.silo.loan.event.LoanRequestedEvent;
import com.silo.loan.repository.LoanRequestRepository;
import com.silo.member.MemberLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanRequestService {

    private final LoanRequestRepository loanRequestRepository;
    private final MemberLookup memberLookup;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LoanRequestResponse submit(UUID memberId, LoanRequestSubmitRequest request) {
        if (!memberLookup.exists(memberId)) {
            throw new ResourceNotFoundException("Member not found with id " + memberId);
        }
        if (!memberLookup.isActiveAndVerified(memberId)) {
            throw new BusinessRuleViolationException(
                    "Member must be ACTIVE and KYC_VERIFIED to submit a loan request");
        }

        LoanRequest loanRequest = LoanRequest.builder()
                .memberId(memberId)
                .amountRequested(request.amountRequested())
                .purpose(request.purpose())
                .status(LoanRequestStatus.PENDING)
                .build();

        loanRequest = loanRequestRepository.save(loanRequest);

        eventPublisher.publishEvent(new LoanRequestedEvent(
                loanRequest.getId(), loanRequest.getMemberId(), loanRequest.getAmountRequested()));

        return toResponse(loanRequest);
    }

    private LoanRequestResponse toResponse(LoanRequest loanRequest) {
        return new LoanRequestResponse(
                loanRequest.getId(),
                loanRequest.getMemberId(),
                loanRequest.getAmountRequested(),
                loanRequest.getPurpose(),
                loanRequest.getStatus(),
                loanRequest.getSubmittedAt());
    }
}
