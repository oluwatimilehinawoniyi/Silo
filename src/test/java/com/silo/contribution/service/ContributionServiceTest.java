package com.silo.contribution.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.contribution.dto.ContributionRequest;
import com.silo.contribution.dto.ContributionResponse;
import com.silo.contribution.dto.ContributionSummaryResponse;
import com.silo.contribution.entity.Contribution;
import com.silo.contribution.entity.ContributionSource;
import com.silo.contribution.event.ContributionMadeEvent;
import com.silo.contribution.repository.ContributionRepository;
import com.silo.member.MemberLookup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock
    private ContributionRepository contributionRepository;

    @Mock
    private MemberLookup memberLookup;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ContributionService contributionService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID OFFICER_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("5000.00");
    private static final String REFERENCE = "cash-receipt-001";

    @Test
    @DisplayName(
            "recordManualContribution rejects a memberId that doesn't exist")
    void recordManualContribution_throwsResourceNotFound_whenMemberDoesNotExist() {
        when(memberLookup.exists(MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(
                () -> contributionService.recordManualContribution(
                        new ContributionRequest(MEMBER_ID, AMOUNT,
                                REFERENCE), OFFICER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(contributionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName(
            "recordManualContribution rejects a member that isn't ACTIVE and KYC_VERIFIED")
    void recordManualContribution_throwsBusinessRuleViolation_whenMemberNotEligible() {
        when(memberLookup.exists(MEMBER_ID)).thenReturn(true);
        when(memberLookup.isActiveAndVerified(MEMBER_ID)).thenReturn(
                false);

        assertThatThrownBy(
                () -> contributionService.recordManualContribution(
                        new ContributionRequest(MEMBER_ID, AMOUNT,
                                REFERENCE), OFFICER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(contributionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName(
            "recordManualContribution saves the contribution and publishes ContributionMadeEvent")
    void recordManualContribution_savesAndPublishesEvent_whenMemberEligible() {
        when(memberLookup.exists(MEMBER_ID)).thenReturn(true);
        when(memberLookup.isActiveAndVerified(MEMBER_ID)).thenReturn(true);
        when(contributionRepository.save(
                any(Contribution.class))).thenAnswer(
                invocation -> invocation.getArgument(0));

        ContributionResponse response =
                contributionService.recordManualContribution(
                        new ContributionRequest(MEMBER_ID, AMOUNT,
                                REFERENCE), OFFICER_ID);

        assertThat(response.memberId()).isEqualTo(MEMBER_ID);
        assertThat(response.amount()).isEqualTo(AMOUNT);
        assertThat(response.reference()).isEqualTo(REFERENCE);
        assertThat(response.source()).isEqualTo(ContributionSource.MANUAL);
        assertThat(response.recordedBy()).isEqualTo(OFFICER_ID);

        ArgumentCaptor<ContributionMadeEvent> captor =
                ArgumentCaptor.forClass(ContributionMadeEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(captor.getValue().getAmount()).isEqualTo(AMOUNT);
        assertThat(captor.getValue().getSource()).isEqualTo(
                ContributionSource.MANUAL);
    }

    @Test
    @DisplayName("getHistory returns contributions in repository order")
    void getHistory_returnsMappedContributions() {
        Contribution contribution = Contribution.builder()
                .id(UUID.randomUUID())
                .memberId(MEMBER_ID)
                .amount(AMOUNT)
                .reference(REFERENCE)
                .source(ContributionSource.MANUAL)
                .recordedBy(OFFICER_ID)
                .build();
        when(contributionRepository.findByMemberIdOrderByContributionDateDesc(
                MEMBER_ID))
                .thenReturn(List.of(contribution));

        List<ContributionResponse> history =
                contributionService.getHistory(MEMBER_ID);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).memberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("getSummary returns the running total and count")
    void getSummary_returnsTotalAndCount() {
        when(contributionRepository.sumAmountByMemberId(
                MEMBER_ID)).thenReturn(new BigDecimal("15000.00"));
        when(contributionRepository.countByMemberId(MEMBER_ID)).thenReturn(
                3L);

        ContributionSummaryResponse summary =
                contributionService.getSummary(MEMBER_ID);

        assertThat(summary.memberId()).isEqualTo(MEMBER_ID);
        assertThat(summary.totalAmount()).isEqualTo(
                new BigDecimal("15000.00"));
        assertThat(summary.contributionCount()).isEqualTo(3L);
    }
}
