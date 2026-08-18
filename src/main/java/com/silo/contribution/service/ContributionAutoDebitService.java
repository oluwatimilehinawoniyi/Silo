package com.silo.contribution.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.contribution.AutoDebitMandateLookup;
import com.silo.contribution.AutoDebitMandateRecorder;
import com.silo.contribution.AutoDebitMandateStatus;
import com.silo.contribution.AutoDebitMandateSummary;
import com.silo.contribution.AutoDebitPeriodicity;
import com.silo.contribution.DueAutoDebitMandate;
import com.silo.contribution.dto.AutoDebitAction;
import com.silo.contribution.dto.AutoDebitMandateUpdateRequest;
import com.silo.contribution.entity.ContributionAutoDebitMandate;
import com.silo.contribution.event.AutoDebitChargeFailedEvent;
import com.silo.contribution.repository.ContributionAutoDebitMandateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContributionAutoDebitService implements AutoDebitMandateLookup, AutoDebitMandateRecorder {

    private static final int FAILURE_THRESHOLD = 3;
    private static final List<AutoDebitMandateStatus> NON_CANCELLED =
            List.of(AutoDebitMandateStatus.ACTIVE, AutoDebitMandateStatus.PAUSED, AutoDebitMandateStatus.FAILED);

    private final ContributionAutoDebitMandateRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AutoDebitMandateSummary createOrReplace(
            UUID memberId, String paystackAuthorizationCode, BigDecimal amount, AutoDebitPeriodicity periodicity) {
        repository.findByMemberIdAndStatusIn(memberId, NON_CANCELLED)
                .ifPresent(existing -> {
                    existing.setStatus(AutoDebitMandateStatus.CANCELLED);
                    repository.save(existing);
                });

        ContributionAutoDebitMandate mandate = ContributionAutoDebitMandate.builder()
                .memberId(memberId)
                .paystackAuthorizationCode(paystackAuthorizationCode)
                .amount(amount)
                .periodicity(periodicity)
                .status(AutoDebitMandateStatus.ACTIVE)
                .nextChargeDate(nextChargeDateFrom(LocalDate.now(), periodicity))
                .build();

        return toSummary(repository.save(mandate));
    }

    public Optional<AutoDebitMandateSummary> getCurrent(UUID memberId) {
        return repository.findByMemberIdAndStatusIn(memberId, NON_CANCELLED).map(this::toSummary);
    }

    @Transactional
    public AutoDebitMandateSummary update(UUID memberId, AutoDebitMandateUpdateRequest request) {
        ContributionAutoDebitMandate mandate = repository.findByMemberIdAndStatusIn(memberId, NON_CANCELLED)
                .orElseThrow(() -> new ResourceNotFoundException("No auto-debit mandate found for this member"));

        if (mandate.getStatus() == AutoDebitMandateStatus.FAILED && request.action() != AutoDebitAction.CANCEL) {
            throw new BusinessRuleViolationException(
                    "This mandate has failed and can only be cancelled - set up a new one to resume auto-debit");
        }

        if (request.action() != null) {
            applyAction(mandate, request.action());
        }

        if (request.amount() != null) {
            mandate.setAmount(request.amount());
        }
        if (request.periodicity() != null) {
            mandate.setPeriodicity(request.periodicity());
            mandate.setNextChargeDate(nextChargeDateFrom(LocalDate.now(), request.periodicity()));
        }

        return toSummary(repository.save(mandate));
    }

    private void applyAction(ContributionAutoDebitMandate mandate, AutoDebitAction action) {
        switch (action) {
            case PAUSE -> {
                if (mandate.getStatus() != AutoDebitMandateStatus.ACTIVE) {
                    throw new BusinessRuleViolationException("Only an ACTIVE mandate can be paused");
                }
                mandate.setStatus(AutoDebitMandateStatus.PAUSED);
            }
            case RESUME -> {
                if (mandate.getStatus() != AutoDebitMandateStatus.PAUSED) {
                    throw new BusinessRuleViolationException("Only a PAUSED mandate can be resumed");
                }
                mandate.setStatus(AutoDebitMandateStatus.ACTIVE);
            }
            case CANCEL -> mandate.setStatus(AutoDebitMandateStatus.CANCELLED);
        }
    }

    @Override
    public List<DueAutoDebitMandate> findDueMandates(LocalDate onOrBefore) {
        return repository.findByStatusAndNextChargeDateLessThanEqual(AutoDebitMandateStatus.ACTIVE, onOrBefore).stream()
                .map(m -> new DueAutoDebitMandate(m.getId(), m.getMemberId(), m.getAmount(), m.getPaystackAuthorizationCode()))
                .toList();
    }

    @Override
    @Transactional
    public void recordChargeSuccess(UUID mandateId) {
        repository.findById(mandateId).ifPresent(mandate -> {
            mandate.setNextChargeDate(nextChargeDateFrom(mandate.getNextChargeDate(), mandate.getPeriodicity()));
            mandate.setConsecutiveFailureCount(0);
            mandate.setLastFailureReason(null);
            repository.save(mandate);
        });
    }

    @Override
    @Transactional
    public void recordChargeFailure(UUID mandateId, String reason) {
        repository.findById(mandateId).ifPresent(mandate -> {
            mandate.setConsecutiveFailureCount(mandate.getConsecutiveFailureCount() + 1);
            mandate.setLastFailureReason(reason);

            boolean mandateStopped = mandate.getConsecutiveFailureCount() >= FAILURE_THRESHOLD;
            if (mandateStopped) {
                mandate.setStatus(AutoDebitMandateStatus.FAILED);
            } else {
                mandate.setNextChargeDate(nextChargeDateFrom(mandate.getNextChargeDate(), mandate.getPeriodicity()));
            }
            repository.save(mandate);

            eventPublisher.publishEvent(new AutoDebitChargeFailedEvent(
                    mandate.getId(), mandate.getMemberId(), mandate.getAmount(), reason, mandateStopped));
        });
    }

    private LocalDate nextChargeDateFrom(LocalDate from, AutoDebitPeriodicity periodicity) {
        return periodicity == AutoDebitPeriodicity.WEEKLY ? from.plusWeeks(1) : from.plusMonths(1);
    }

    private AutoDebitMandateSummary toSummary(ContributionAutoDebitMandate mandate) {
        return new AutoDebitMandateSummary(
                mandate.getId(), mandate.getAmount(), mandate.getPeriodicity(), mandate.getStatus(),
                mandate.getNextChargeDate(), mandate.getConsecutiveFailureCount(), mandate.getLastFailureReason());
    }
}
