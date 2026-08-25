package com.silo.accounting.service;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.entity.LedgerEntry;
import com.silo.accounting.enums.AccountType;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.repayment.event.RepaymentMadeEvent;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepaymentLedgerListenerTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private RepaymentLedgerListener listener;

    private static final UUID REPAYMENT_ID = UUID.randomUUID();
    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID PAYER_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("2500.00");

    private final LedgerAccount cashAndBank = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("1000").name("Cash and Bank").type(AccountType.ASSET).active(true).build();
    private final LedgerAccount loansReceivable = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("1100").name("Loans Receivable").type(AccountType.ASSET).active(true).build();
    private final LedgerAccount guarantorReceivable = LedgerAccount.builder()
            .id(UUID.randomUUID()).code("1200").name("Guarantor Receivable").type(AccountType.ASSET).active(true).build();

    @Test
    @DisplayName("borrower repayment (liabilityId null) debits Cash and Bank, credits Loans Receivable")
    void onRepaymentMade_postsLoanRepaymentEntries_whenNotLiabilityPayment() {
        RepaymentMadeEvent event = new RepaymentMadeEvent(REPAYMENT_ID, LOAN_ID, PAYER_ID, AMOUNT, null);
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of());
        when(ledgerAccountRepository.findByCode("1000")).thenReturn(Optional.of(cashAndBank));
        when(ledgerAccountRepository.findByCode("1100")).thenReturn(Optional.of(loansReceivable));

        listener.onRepaymentMade(event);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(captor.capture());
        List<LedgerEntry> saved = captor.getAllValues();

        assertThat(saved.get(0).getEntryType()).isEqualTo(EntryType.DEBIT);
        assertThat(saved.get(0).getAccount()).isEqualTo(cashAndBank);
        assertThat(saved.get(1).getEntryType()).isEqualTo(EntryType.CREDIT);
        assertThat(saved.get(1).getAccount()).isEqualTo(loansReceivable);
        saved.forEach(entry -> assertThat(entry.getAmount()).isEqualTo(AMOUNT));
    }

    @Test
    @DisplayName("guarantor liability repayment (liabilityId set) debits Cash and Bank, credits Guarantor Receivable")
    void onRepaymentMade_postsLiabilityRepaymentEntries_whenLiabilityPayment() {
        UUID liabilityId = UUID.randomUUID();
        RepaymentMadeEvent event = new RepaymentMadeEvent(REPAYMENT_ID, LOAN_ID, PAYER_ID, AMOUNT, liabilityId);
        when(ledgerEntryRepository.findByTransactionId(event.getEventId())).thenReturn(List.of());
        when(ledgerAccountRepository.findByCode("1000")).thenReturn(Optional.of(cashAndBank));
        when(ledgerAccountRepository.findByCode("1200")).thenReturn(Optional.of(guarantorReceivable));

        listener.onRepaymentMade(event);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(2)).save(captor.capture());
        List<LedgerEntry> saved = captor.getAllValues();

        assertThat(saved.get(0).getAccount()).isEqualTo(cashAndBank);
        assertThat(saved.get(1).getAccount()).isEqualTo(guarantorReceivable);
    }

    @Test
    @DisplayName("skips posting when entries for this event were already recorded (outbox redelivery)")
    void onRepaymentMade_skipsWhenAlreadyPosted() {
        RepaymentMadeEvent event = new RepaymentMadeEvent(REPAYMENT_ID, LOAN_ID, PAYER_ID, AMOUNT, null);
        when(ledgerEntryRepository.findByTransactionId(event.getEventId()))
                .thenReturn(List.of(LedgerEntry.builder().id(UUID.randomUUID()).build()));

        listener.onRepaymentMade(event);

        verify(ledgerAccountRepository, never()).findByCode(anyString());
        verify(ledgerEntryRepository, never()).save(any());
    }
}
