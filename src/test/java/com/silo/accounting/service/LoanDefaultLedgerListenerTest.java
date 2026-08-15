package com.silo.accounting.service;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.entity.LedgerEntry;
import com.silo.accounting.enums.AccountType;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.loan.event.GuarantorLiabilityAllocation;
import com.silo.loan.event.LoanDefaultedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanDefaultLedgerListenerTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    private LoanDefaultLedgerListener listener;

    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID GUARANTOR_ID = UUID.randomUUID();

    private final LedgerAccount guarantorReceivable = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("1200").name("Guarantor Receivable").type(AccountType.ASSET).active(true).build();
    private final LedgerAccount loansReceivable = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("1100").name("Loans Receivable").type(AccountType.ASSET).active(true).build();
    private final LedgerAccount penaltyReceivable = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("1300").name("Penalty Receivable").type(AccountType.ASSET).active(true).build();
    private final LedgerAccount penaltyIncome = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("4100").name("Penalty Income").type(AccountType.INCOME).active(true).build();

    @BeforeEach
    void setUp() {
        listener = new LoanDefaultLedgerListener(
                ledgerAccountRepository, ledgerEntryRepository, new BigDecimal("0.05"));
    }

    @Test
    @DisplayName("posts a guarantor receivable transfer per allocation plus one penalty entry, 5% of outstanding balance")
    void onLoanDefaulted_postsTransfersAndPenalty() {
        LoanDefaultedEvent event = new LoanDefaultedEvent(
                LOAN_ID, MEMBER_ID, new BigDecimal("10000.00"),
                List.of(new GuarantorLiabilityAllocation(UUID.randomUUID(), GUARANTOR_ID, new BigDecimal("10000.00"))));
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of());
        when(ledgerAccountRepository.findByCode("1200")).thenReturn(Optional.of(guarantorReceivable));
        when(ledgerAccountRepository.findByCode("1100")).thenReturn(Optional.of(loansReceivable));
        when(ledgerAccountRepository.findByCode("1300")).thenReturn(Optional.of(penaltyReceivable));
        when(ledgerAccountRepository.findByCode("4100")).thenReturn(Optional.of(penaltyIncome));

        listener.onLoanDefaulted(event);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(4)).save(captor.capture());
        List<LedgerEntry> saved = captor.getAllValues();

        assertThat(saved.get(0).getAccount()).isEqualTo(guarantorReceivable);
        assertThat(saved.get(0).getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(saved.get(0).getAmount()).isEqualTo(new BigDecimal("10000.00"));

        assertThat(saved.get(1).getAccount()).isEqualTo(loansReceivable);
        assertThat(saved.get(1).getEntryType()).isEqualTo(EntryType.CREDIT);

        assertThat(saved.get(2).getAccount()).isEqualTo(penaltyReceivable);
        assertThat(saved.get(2).getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(saved.get(2).getAmount()).isEqualTo(new BigDecimal("500.00"));

        assertThat(saved.get(3).getAccount()).isEqualTo(penaltyIncome);
        assertThat(saved.get(3).getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(saved.get(3).getAmount()).isEqualTo(new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("posts only the penalty entries when there are no guarantor liability allocations")
    void onLoanDefaulted_postsOnlyPenalty_whenNoAllocations() {
        LoanDefaultedEvent event = new LoanDefaultedEvent(
                LOAN_ID, MEMBER_ID, new BigDecimal("10000.00"), List.of());
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of());
        when(ledgerAccountRepository.findByCode("1200")).thenReturn(Optional.of(guarantorReceivable));
        when(ledgerAccountRepository.findByCode("1100")).thenReturn(Optional.of(loansReceivable));
        when(ledgerAccountRepository.findByCode("1300")).thenReturn(Optional.of(penaltyReceivable));
        when(ledgerAccountRepository.findByCode("4100")).thenReturn(Optional.of(penaltyIncome));

        listener.onLoanDefaulted(event);

        verify(ledgerEntryRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("skips posting when entries for this event were already recorded (outbox redelivery)")
    void onLoanDefaulted_skipsWhenAlreadyPosted() {
        LoanDefaultedEvent event = new LoanDefaultedEvent(
                LOAN_ID, MEMBER_ID, new BigDecimal("10000.00"), List.of());
        when(ledgerEntryRepository.findByTransactionId(event.getEventId()))
                .thenReturn(List.of(LedgerEntry.builder().id(UUID.randomUUID()).build()));

        listener.onLoanDefaulted(event);

        verify(ledgerAccountRepository, never()).findByCode(anyString());
        verify(ledgerEntryRepository, never()).save(any());
    }
}
