package com.silo.accounting.service;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.entity.LedgerEntry;
import com.silo.accounting.enums.AccountType;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.loan.event.LoanApprovedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanDisbursementLedgerListenerTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LoanDisbursementLedgerListener listener;

    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID LOAN_REQUEST_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final BigDecimal PRINCIPAL = new BigDecimal("50000.00");

    private final LedgerAccount loansReceivable = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("1100").name("Loans Receivable").type(AccountType.ASSET).active(true).build();
    private final LedgerAccount cashAndBank = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("1000").name("Cash and Bank").type(AccountType.ASSET).active(true).build();

    @Test
    @DisplayName("posts a balanced debit to Loans Receivable and credit to Cash and Bank")
    void onLoanApproved_postsBalancedEntries() {
        LoanApprovedEvent event = new LoanApprovedEvent(
                LOAN_ID, LOAN_REQUEST_ID, MEMBER_ID, PRINCIPAL, LocalDateTime.now());
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of());
        when(ledgerAccountRepository.findByCode("1100")).thenReturn(Optional.of(loansReceivable));
        when(ledgerAccountRepository.findByCode("1000")).thenReturn(Optional.of(cashAndBank));

        listener.onLoanApproved(event);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(captor.capture());

        List<LedgerEntry> saved = captor.getAllValues();
        LedgerEntry debit = saved.get(0);
        LedgerEntry credit = saved.get(1);

        assertThat(debit.getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(debit.getAccount()).isEqualTo(loansReceivable);
        assertThat(debit.getAmount()).isEqualTo(PRINCIPAL);
        assertThat(debit.getTransactionId()).isEqualTo(event.getEventId());

        assertThat(credit.getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(credit.getAccount()).isEqualTo(cashAndBank);
        assertThat(credit.getAmount()).isEqualTo(PRINCIPAL);
        assertThat(credit.getTransactionId()).isEqualTo(event.getEventId());
    }

    @Test
    @DisplayName("skips posting when entries for this event were already recorded (outbox redelivery)")
    void onLoanApproved_skipsWhenAlreadyPosted() {
        LoanApprovedEvent event = new LoanApprovedEvent(
                LOAN_ID, LOAN_REQUEST_ID, MEMBER_ID, PRINCIPAL, LocalDateTime.now());
        LedgerEntry existing = LedgerEntry.builder().id(UUID.randomUUID()).build();
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of(existing));

        listener.onLoanApproved(event);

        verify(ledgerAccountRepository, never()).findByCode(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("fails loudly when the chart of accounts is missing an expected code")
    void onLoanApproved_throwsIllegalState_whenAccountMissing() {
        LoanApprovedEvent event = new LoanApprovedEvent(
                LOAN_ID, LOAN_REQUEST_ID, MEMBER_ID, PRINCIPAL, LocalDateTime.now());
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of());
        when(ledgerAccountRepository.findByCode("1100")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listener.onLoanApproved(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1100");

        verify(ledgerEntryRepository, never()).save(any());
    }
}
