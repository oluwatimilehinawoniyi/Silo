package com.silo.loan.service;

import com.silo.loan.dto.AvailableGuarantorResponse;
import com.silo.loan.dto.GuarantorInviteResponse;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.LoanRequestStatus;
import com.silo.loan.enums.RiskTier;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanRequestRepository;
import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuarantorDiscoveryServiceTest {

    @Mock
    private MemberLookup memberLookup;

    @Mock
    private GuarantorCredibilityService guarantorCredibilityService;

    @Mock
    private LoanGuarantorRepository loanGuarantorRepository;

    @Mock
    private LoanRequestRepository loanRequestRepository;

    @Mock
    private BorrowerRiskService borrowerRiskService;

    @InjectMocks
    private GuarantorDiscoveryService guarantorDiscoveryService;

    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID OTHER_MEMBER_ID = UUID.randomUUID();

    @Test
    @DisplayName("getAvailableGuarantors excludes the caller and attaches each member's credibility score")
    void getAvailableGuarantors_excludesCallerAndAttachesScore() {
        MemberSummary caller = new MemberSummary(CALLER_ID, "caller@example.com");
        MemberSummary other = new MemberSummary(OTHER_MEMBER_ID, "other@example.com");
        when(memberLookup.findAllActiveAndVerified()).thenReturn(List.of(caller, other));
        when(guarantorCredibilityService.getCurrentScore(OTHER_MEMBER_ID)).thenReturn(80);

        List<AvailableGuarantorResponse> result = guarantorDiscoveryService.getAvailableGuarantors(CALLER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).memberId()).isEqualTo(OTHER_MEMBER_ID);
        assertThat(result.get(0).credibilityScore()).isEqualTo(80);
    }

    @Test
    @DisplayName("getPendingInvites enriches each PENDING invite with requester details and risk tier")
    void getPendingInvites_enrichesWithRequesterDetailsAndRiskTier() {
        UUID loanRequestId = UUID.randomUUID();
        UUID borrowerId = UUID.randomUUID();
        LoanGuarantor invite = LoanGuarantor.builder()
                .id(UUID.randomUUID())
                .loanRequestId(loanRequestId)
                .memberId(OTHER_MEMBER_ID)
                .status(GuarantorStatus.PENDING)
                .invitedAt(LocalDateTime.now())
                .build();
        LoanRequest loanRequest = LoanRequest.builder()
                .id(loanRequestId)
                .memberId(borrowerId)
                .amountRequested(new BigDecimal("20000.00"))
                .purpose("School fees")
                .status(LoanRequestStatus.PENDING)
                .build();
        when(loanGuarantorRepository.findByMemberIdAndStatus(OTHER_MEMBER_ID, GuarantorStatus.PENDING))
                .thenReturn(List.of(invite));
        when(loanRequestRepository.findById(loanRequestId)).thenReturn(Optional.of(loanRequest));
        when(borrowerRiskService.getCurrentTier(borrowerId)).thenReturn(RiskTier.LOW);

        List<GuarantorInviteResponse> result = guarantorDiscoveryService.getPendingInvites(OTHER_MEMBER_ID);

        assertThat(result).hasSize(1);
        GuarantorInviteResponse response = result.get(0);
        assertThat(response.loanRequestId()).isEqualTo(loanRequestId);
        assertThat(response.borrowerMemberId()).isEqualTo(borrowerId);
        assertThat(response.amountRequested()).isEqualTo(new BigDecimal("20000.00"));
        assertThat(response.purpose()).isEqualTo("School fees");
        assertThat(response.borrowerRiskTier()).isEqualTo(RiskTier.LOW);
    }

    @Test
    @DisplayName("getPendingInvites fails loudly if the referenced loan request is missing")
    void getPendingInvites_throwsIllegalState_whenLoanRequestMissing() {
        UUID loanRequestId = UUID.randomUUID();
        LoanGuarantor invite = LoanGuarantor.builder()
                .id(UUID.randomUUID())
                .loanRequestId(loanRequestId)
                .memberId(OTHER_MEMBER_ID)
                .status(GuarantorStatus.PENDING)
                .build();
        when(loanGuarantorRepository.findByMemberIdAndStatus(OTHER_MEMBER_ID, GuarantorStatus.PENDING))
                .thenReturn(List.of(invite));
        when(loanRequestRepository.findById(loanRequestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guarantorDiscoveryService.getPendingInvites(OTHER_MEMBER_ID))
                .isInstanceOf(IllegalStateException.class);
    }
}
