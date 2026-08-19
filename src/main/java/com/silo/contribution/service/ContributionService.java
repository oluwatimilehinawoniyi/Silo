package com.silo.contribution.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.contribution.ContributionRecorder;
import com.silo.contribution.dto.ContributionRequest;
import com.silo.contribution.dto.ContributionResponse;
import com.silo.contribution.dto.ContributionSummaryResponse;
import com.silo.contribution.entity.Contribution;
import com.silo.contribution.entity.ContributionSource;
import com.silo.contribution.event.ContributionMadeEvent;
import com.silo.contribution.repository.ContributionRepository;
import com.silo.member.MemberLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContributionService implements ContributionRecorder {

    private final ContributionRepository contributionRepository;
    private final MemberLookup memberLookup;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ContributionResponse recordManualContribution(
            ContributionRequest request, UUID recordedBy) {
        if (request.memberId().equals(recordedBy)) {
            throw new BusinessRuleViolationException("An officer cannot record their own contribution");
        }
        assertEligible(request.memberId());

        Contribution contribution = Contribution.builder()
                .memberId(request.memberId())
                .amount(request.amount())
                .reference(request.reference())
                .source(ContributionSource.MANUAL)
                .recordedBy(recordedBy)
                .build();

        contribution = contributionRepository.save(contribution);

        eventPublisher.publishEvent(new ContributionMadeEvent(
                contribution.getId(), contribution.getMemberId(),
                contribution.getAmount(), contribution.getSource()));

        return toResponse(contribution);
    }

    @Override
    @Transactional
    public void recordPaystackContribution(UUID memberId, BigDecimal amount, String reference) {
        assertEligible(memberId);

        Contribution contribution = Contribution.builder()
                .memberId(memberId)
                .amount(amount)
                .reference(reference)
                .source(ContributionSource.PAYSTACK)
                .recordedBy(null)
                .build();

        contribution = contributionRepository.save(contribution);

        eventPublisher.publishEvent(new ContributionMadeEvent(
                contribution.getId(), contribution.getMemberId(),
                contribution.getAmount(), contribution.getSource()));
    }

    public List<ContributionResponse> getAll() {
        return contributionRepository.findAllByOrderByContributionDateDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ContributionResponse> getHistory(UUID memberId) {
        return contributionRepository.findByMemberIdOrderByContributionDateDesc(
                        memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ContributionSummaryResponse getSummary(UUID memberId) {
        return new ContributionSummaryResponse(
                memberId,
                contributionRepository.sumAmountByMemberId(memberId),
                contributionRepository.countByMemberId(memberId));
    }

    private void assertEligible(UUID memberId) {
        if (!memberLookup.exists(memberId)) {
            throw new ResourceNotFoundException(
                    "Member not found with id " + memberId);
        }
        if (!memberLookup.isActiveAndVerified(memberId)) {
            throw new BusinessRuleViolationException(
                    "Member must be ACTIVE and KYC_VERIFIED to contribute");
        }
    }

    private ContributionResponse toResponse(Contribution contribution) {
        return new ContributionResponse(
                contribution.getId(),
                contribution.getMemberId(),
                contribution.getAmount(),
                contribution.getReference(),
                contribution.getSource(),
                contribution.getRecordedBy(),
                contribution.getContributionDate());
    }
}
