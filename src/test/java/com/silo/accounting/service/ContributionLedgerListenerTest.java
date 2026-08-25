package com.silo.accounting.service;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.entity.LedgerEntry;
import com.silo.accounting.enums.AccountType;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.contribution.entity.ContributionSource;
import com.silo.contribution.event.ContributionMadeEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributionLedgerListenerTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private ContributionLedgerListener listener;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID CONTRIBUTION_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("5000.00");

    private final LedgerAccount cashAndBank = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("1000").name("Cash and Bank").type(AccountType.ASSET).active(true).build();
    private final LedgerAccount cooperativeFundEquity = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("3000").name("Cooperative Fund Equity").type(AccountType.EQUITY)
            .active(true).build();

    @Test
    @DisplayName("posts a balanced debit to Cash and Bank and credit to Cooperative Fund Equity")
    void onContributionMade_postsBalancedEntries() {
        ContributionMadeEvent event = new ContributionMadeEvent(
                CONTRIBUTION_ID, MEMBER_ID, AMOUNT, ContributionSource.MANUAL);
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of());
        when(ledgerAccountRepository.findByCode("1000")).thenReturn(Optional.of(cashAndBank));
        when(ledgerAccountRepository.findByCode("3000")).thenReturn(Optional.of(cooperativeFundEquity));

        listener.onContributionMade(event);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(captor.capture());

        List<LedgerEntry> saved = captor.getAllValues();
        LedgerEntry debit = saved.get(0);
        LedgerEntry credit = saved.get(1);

        assertThat(debit.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(debit.getAccount()).isEqualTo(cashAndBank);
        assertThat(debit.getAmount()).isEqualTo(AMOUNT);
        assertThat(debit.getTransactionId()).isEqualTo(event.getEventId());

        assertThat(credit.getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(credit.getAccount()).isEqualTo(cooperativeFundEquity);
        assertThat(credit.getAmount()).isEqualTo(AMOUNT);
        assertThat(credit.getTransactionId()).isEqualTo(event.getEventId());
    }

    @Test
    @DisplayName("skips posting when entries for this event were already recorded (outbox redelivery)")
    void onContributionMade_skipsWhenAlreadyPosted() {
        ContributionMadeEvent event = new ContributionMadeEvent(
                CONTRIBUTION_ID, MEMBER_ID, AMOUNT, ContributionSource.MANUAL);
        LedgerEntry existing = LedgerEntry.builder().id(UUID.randomUUID()).build();
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of(existing));

        listener.onContributionMade(event);

        verify(ledgerAccountRepository, never()).findByCode(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails loudly when the chart of accounts is missing an expected code")
    void onContributionMade_throwsIllegalState_whenAccountMissing() {
        ContributionMadeEvent event = new ContributionMadeEvent(
                CONTRIBUTION_ID, MEMBER_ID, AMOUNT, ContributionSource.MANUAL);
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of());
        when(ledgerAccountRepository.findByCode("1000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.onContributionMade(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1000");

        verify(ledgerEntryRepository, never()).save(any());
    }
}
