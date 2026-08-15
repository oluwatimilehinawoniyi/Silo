package com.silo.accounting.service;

import com.silo.accounting.dto.LedgerAccountBalanceResponse;
import com.silo.accounting.dto.TrialBalanceEntryResponse;
import com.silo.accounting.dto.TrialBalanceResponse;
import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.enums.NormalBalance;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerQueryService {

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerAccountBalanceResponse getAccountBalance(UUID accountId) {
        LedgerAccount account = ledgerAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Ledger account not found with id " + accountId));

        BigDecimal balance = computeBalance(account);

        return new LedgerAccountBalanceResponse(account.getId(), account.getCode(), account.getName(), balance);
    }

    public TrialBalanceResponse getTrialBalance() {
        List<TrialBalanceEntryResponse> entries = ledgerAccountRepository.findAll().stream()
                .map(this::toTrialBalanceEntry)
                .toList();

        BigDecimal totalDebits = entries.stream()
                .map(TrialBalanceEntryResponse::totalDebits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entries.stream()
                .map(TrialBalanceEntryResponse::totalCredits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TrialBalanceResponse(entries, totalDebits, totalCredits);
    }

    private TrialBalanceEntryResponse toTrialBalanceEntry(LedgerAccount account) {
        BigDecimal debits = ledgerEntryRepository.sumAmountByAccountIdAndEntryType(account.getId(), EntryType.DEBIT);
        BigDecimal credits = ledgerEntryRepository.sumAmountByAccountIdAndEntryType(account.getId(), EntryType.CREDIT);
        return new TrialBalanceEntryResponse(account.getId(), account.getCode(), account.getName(), debits, credits);
    }

    private BigDecimal computeBalance(LedgerAccount account) {
        BigDecimal debits = ledgerEntryRepository.sumAmountByAccountIdAndEntryType(account.getId(), EntryType.DEBIT);
        BigDecimal credits = ledgerEntryRepository.sumAmountByAccountIdAndEntryType(account.getId(), EntryType.CREDIT);

        return account.getType().getNormalBalance() == NormalBalance.DEBIT
                ? debits.subtract(credits)
                : credits.subtract(debits);
    }
}
