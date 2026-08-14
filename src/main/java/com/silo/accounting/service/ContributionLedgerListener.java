package com.silo.accounting.service;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.entity.LedgerEntry;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.contribution.event.ContributionMadeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContributionLedgerListener {

    private static final String CASH_AND_BANK_CODE = "1000";
    private static final String COOPERATIVE_FUND_EQUITY_CODE = "3000";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onContributionMade(ContributionMadeEvent event) {
        if (!ledgerEntryRepository.findByTransactionId(event.getEventId()).isEmpty()) {
            log.info("Ledger entries already posted for contribution event {}, skipping", event.getEventId());
            return;
        }

        LedgerAccount cashAndBank = requireAccount(CASH_AND_BANK_CODE);
        LedgerAccount cooperativeFundEquity = requireAccount(COOPERATIVE_FUND_EQUITY_CODE);

        String description = "Contribution " + event.getContributionId() + " from member " + event.getMemberId();

        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId(event.getEventId())
                .account(cashAndBank)
                .entryType(EntryType.DEBIT)
                .amount(event.getAmount())
                .description(description)
                .build());

        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId(event.getEventId())
                .account(cooperativeFundEquity)
                .entryType(EntryType.CREDIT)
                .amount(event.getAmount())
                .description(description)
                .build());
    }

    private LedgerAccount requireAccount(String code) {
        return ledgerAccountRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Chart of accounts missing account code " + code));
    }
}
