package com.silo.contribution.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.contribution.AutoDebitMandateStatus;
import com.silo.contribution.AutoDebitMandateSummary;
import com.silo.contribution.AutoDebitPeriodicity;
import com.silo.contribution.DueAutoDebitMandate;
import com.silo.contribution.dto.AutoDebitAction;
import com.silo.contribution.dto.AutoDebitMandateUpdateRequest;
import com.silo.contribution.entity.ContributionAutoDebitMandate;
import com.silo.contribution.event.AutoDebitChargeFailedEvent;
import com.silo.contribution.repository.ContributionAutoDebitMandateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributionAutoDebitServiceTest {

    @Mock
    private ContributionAutoDebitMandateRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ContributionAutoDebitService service;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID MANDATE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ContributionAutoDebitService(repository, eventPublisher);
    }

    private ContributionAutoDebitMandate activeMandate() {
        return ContributionAutoDebitMandate.builder()
                .id(MANDATE_ID).memberId(MEMBER_ID).paystackAuthorizationCode("AUTH_123")
                .amount(new BigDecimal("5000.00")).periodicity(AutoDebitPeriodicity.MONTHLY)
                .status(AutoDebitMandateStatus.ACTIVE).nextChargeDate(LocalDate.now().plusDays(30))
                .consecutiveFailureCount(0).build();
    }

    @Test
    @DisplayName("createOrReplace cancels an existing non-cancelled mandate before creating the new one")
    void createOrReplace_cancelsExisting() {
        ContributionAutoDebitMandate existing = activeMandate();
        when(repository.findByMemberIdAndStatusIn(eq(MEMBER_ID), any())).thenReturn(Optional.of(existing));
        when(repository.save(any(ContributionAutoDebitMandate.class))).thenAnswer(inv -> inv.getArgument(0));

        AutoDebitMandateSummary summary = service.createOrReplace(
                MEMBER_ID, "AUTH_456", new BigDecimal("3000.00"), AutoDebitPeriodicity.WEEKLY);

        assertThat(existing.getStatus()).isEqualTo(AutoDebitMandateStatus.CANCELLED);
        assertThat(summary.amount()).isEqualByComparingTo("3000.00");
        assertThat(summary.periodicity()).isEqualTo(AutoDebitPeriodicity.WEEKLY);
        assertThat(summary.status()).isEqualTo(AutoDebitMandateStatus.ACTIVE);
    }

    @Test
    @DisplayName("update rejects pausing a mandate that isn't ACTIVE")
    void update_rejectsPause_whenNotActive() {
        ContributionAutoDebitMandate mandate = activeMandate();
        mandate.setStatus(AutoDebitMandateStatus.PAUSED);
        when(repository.findByMemberIdAndStatusIn(eq(MEMBER_ID), any())).thenReturn(Optional.of(mandate));

        assertThatThrownBy(() -> service.update(MEMBER_ID, new AutoDebitMandateUpdateRequest(null, null, AutoDebitAction.PAUSE)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("update rejects any change on a FAILED mandate other than cancelling it")
    void update_rejectsChanges_onFailedMandate_unlessCancelling() {
        ContributionAutoDebitMandate mandate = activeMandate();
        mandate.setStatus(AutoDebitMandateStatus.FAILED);
        when(repository.findByMemberIdAndStatusIn(eq(MEMBER_ID), any())).thenReturn(Optional.of(mandate));

        assertThatThrownBy(() -> service.update(MEMBER_ID, new AutoDebitMandateUpdateRequest(new BigDecimal("100"), null, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("update rejects when no mandate exists for the member")
    void update_throwsResourceNotFound_whenNoMandate() {
        when(repository.findByMemberIdAndStatusIn(eq(MEMBER_ID), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(MEMBER_ID, new AutoDebitMandateUpdateRequest(null, null, AutoDebitAction.CANCEL)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("findDueMandates maps only ACTIVE mandates due on or before the given date")
    void findDueMandates_mapsActiveMandates() {
        ContributionAutoDebitMandate mandate = activeMandate();
        when(repository.findByStatusAndNextChargeDateLessThanEqual(AutoDebitMandateStatus.ACTIVE, LocalDate.now()))
                .thenReturn(List.of(mandate));

        List<DueAutoDebitMandate> due = service.findDueMandates(LocalDate.now());

        assertThat(due).hasSize(1);
        assertThat(due.get(0).mandateId()).isEqualTo(MANDATE_ID);
        assertThat(due.get(0).paystackAuthorizationCode()).isEqualTo("AUTH_123");
    }

    @Test
    @DisplayName("recordChargeSuccess advances nextChargeDate and clears the failure streak")
    void recordChargeSuccess_advancesAndClearsFailures() {
        ContributionAutoDebitMandate mandate = activeMandate();
        mandate.setConsecutiveFailureCount(2);
        mandate.setLastFailureReason("Insufficient funds");
        LocalDate previousChargeDate = mandate.getNextChargeDate();
        when(repository.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));

        service.recordChargeSuccess(MANDATE_ID);

        assertThat(mandate.getConsecutiveFailureCount()).isZero();
        assertThat(mandate.getLastFailureReason()).isNull();
        assertThat(mandate.getNextChargeDate()).isEqualTo(previousChargeDate.plusMonths(1));
    }

    @Test
    @DisplayName("recordChargeFailure below the threshold keeps the mandate ACTIVE and reschedules")
    void recordChargeFailure_belowThreshold_keepsActive() {
        ContributionAutoDebitMandate mandate = activeMandate();
        when(repository.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));

        service.recordChargeFailure(MANDATE_ID, "Card declined");

        assertThat(mandate.getStatus()).isEqualTo(AutoDebitMandateStatus.ACTIVE);
        assertThat(mandate.getConsecutiveFailureCount()).isEqualTo(1);

        ArgumentCaptor<AutoDebitChargeFailedEvent> captor = ArgumentCaptor.forClass(AutoDebitChargeFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isMandateStopped()).isFalse();
    }

    @Test
    @DisplayName("recordChargeFailure at the threshold flips the mandate to FAILED and stops retrying")
    void recordChargeFailure_atThreshold_flipsToFailed() {
        ContributionAutoDebitMandate mandate = activeMandate();
        mandate.setConsecutiveFailureCount(2);
        when(repository.findById(MANDATE_ID)).thenReturn(Optional.of(mandate));

        service.recordChargeFailure(MANDATE_ID, "Card declined");

        assertThat(mandate.getStatus()).isEqualTo(AutoDebitMandateStatus.FAILED);
        assertThat(mandate.getConsecutiveFailureCount()).isEqualTo(3);

        ArgumentCaptor<AutoDebitChargeFailedEvent> captor = ArgumentCaptor.forClass(AutoDebitChargeFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().isMandateStopped()).isTrue();
    }
}
