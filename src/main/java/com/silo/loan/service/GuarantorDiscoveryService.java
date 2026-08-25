package com.silo.loan.service;

import com.silo.loan.dto.AvailableGuarantorResponse;
import com.silo.loan.dto.GuarantorInviteResponse;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanRequestRepository;
import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuarantorDiscoveryService {

    private final MemberLookup memberLookup;
    private final GuarantorCredibilityService guarantorCredibilityService;
    private final LoanGuarantorRepository loanGuarantorRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final BorrowerRiskService borrowerRiskService;

    public List<AvailableGuarantorResponse> getAvailableGuarantors(UUID excludingMemberId) {
        return memberLookup.findAllActiveAndVerified()
                .stream()
                .filter(member -> !member.id().equals(excludingMemberId))
                .map(this::toAvailableGuarantorResponse)
                .toList();
    }

    public List<GuarantorInviteResponse> getPendingInvites(UUID guarantorMemberId) {
        return loanGuarantorRepository.findByMemberIdAndStatus(guarantorMemberId, GuarantorStatus.PENDING)
                .stream()
                .map(this::toInviteResponse)
                .toList();
    }

    private AvailableGuarantorResponse toAvailableGuarantorResponse(MemberSummary member) {
        return new AvailableGuarantorResponse(
                member.id(), member.email(), guarantorCredibilityService.getCurrentScore(member.id()));
    }

    private GuarantorInviteResponse toInviteResponse(LoanGuarantor guarantor) {
        LoanRequest loanRequest = loanRequestRepository.findById(guarantor.getLoanRequestId())
                .orElseThrow(() -> new IllegalStateException(
                        "Loan request not found for guarantor invite " + guarantor.getId()));

        return new GuarantorInviteResponse(
                guarantor.getId(),
                loanRequest.getId(),
                loanRequest.getMemberId(),
                loanRequest.getAmountRequested(),
                loanRequest.getPurpose(),
                borrowerRiskService.getCurrentTier(loanRequest.getMemberId()),
                guarantor.getInvitedAt());
    }
}
