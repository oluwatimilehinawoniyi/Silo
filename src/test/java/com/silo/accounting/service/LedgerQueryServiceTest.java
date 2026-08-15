package com.silo.accounting.service;

import com.silo.accounting.dto.LedgerAccountBalanceResponse;
import com.silo.accounting.dto.TrialBalanceResponse;
import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.enums.AccountType;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerQueryServiceTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerQueryService ledgerQueryService;

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Test
    @DisplayName("getAccountBalance rejects an account that doesn't exist")
    void getAccountBalance_throwsResourceNotFound_whenAccountMissing() {
        when(ledgerAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ledgerQueryService.getAccountBalance(ACCOUNT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getAccountBalance is debits minus credits for a DEBIT-normal account")
    void getAccountBalance_debitsMinusCredits_forAssetAccount() {
        LedgerAccount cashAndBank = LedgerAccount.builder()
                .id(ACCOUNT_ID).code("1000").name("Cash and Bank").type(AccountType.ASSET).active(true).build();
        when(ledgerAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(cashAndBank));
        when(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(ACCOUNT_ID, EntryType.DEBIT))
                .thenReturn(new BigDecimal("10000.00"));
        when(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(ACCOUNT_ID, EntryType.CREDIT))
                .thenReturn(new BigDecimal("3000.00"));

        LedgerAccountBalanceResponse response = ledgerQueryService.getAccountBalance(ACCOUNT_ID);

        assertThat(response.balance()).isEqualTo(new BigDecimal("7000.00"));
    }

    @Test
    @DisplayName("getAccountBalance is credits minus debits for a CREDIT-normal account")
    void getAccountBalance_creditsMinusDebits_forEquityAccount() {
        LedgerAccount fundEquity = LedgerAccount.builder()
                .id(ACCOUNT_ID).code("3000").name("Cooperative Fund Equity").type(AccountType.EQUITY).active(true).build();
        when(ledgerAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(fundEquity));
        when(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(ACCOUNT_ID, EntryType.DEBIT))
                .thenReturn(new BigDecimal("500.00"));
        when(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(ACCOUNT_ID, EntryType.CREDIT))
                .thenReturn(new BigDecimal("8000.00"));

        LedgerAccountBalanceResponse response = ledgerQueryService.getAccountBalance(ACCOUNT_ID);

        assertThat(response.balance()).isEqualTo(new BigDecimal("7500.00"));
    }

    @Test
    @DisplayName("getTrialBalance sums debits and credits across every account")
    void getTrialBalance_sumsAcrossAllAccounts() {
        LedgerAccount cashAndBank = LedgerAccount.builder()
                .id(UUID.randomUUID()).code("1000").name("Cash and Bank").type(AccountType.ASSET).active(true).build();
        LedgerAccount fundEquity = LedgerAccount.builder()
                .id(UUID.randomUUID()).code("3000").name("Cooperative Fund Equity").type(AccountType.EQUITY).active(true).build();
        when(ledgerAccountRepository.findAll()).thenReturn(List.of(cashAndBank, fundEquity));
        when(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(cashAndBank.getId(), EntryType.DEBIT))
                .thenReturn(new BigDecimal("5000.00"));
        when(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(cashAndBank.getId(), EntryType.CREDIT))
                .thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(fundEquity.getId(), EntryType.DEBIT))
                .thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(fundEquity.getId(), EntryType.CREDIT))
                .thenReturn(new BigDecimal("5000.00"));

        TrialBalanceResponse trialBalance = ledgerQueryService.getTrialBalance();

        assertThat(trialBalance.accounts()).hasSize(2);
        assertThat(trialBalance.totalDebits()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(trialBalance.totalCredits()).isEqualTo(new BigDecimal("5000.00"));
    }
}
