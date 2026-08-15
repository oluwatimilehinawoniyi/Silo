package com.silo.accounting.service;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.entity.LedgerEntry;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.repayment.event.RepaymentMadeEvent;
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
public class RepaymentLedgerListener {

    private static final String CASH_AND_BANK_CODE = "1000";
    private static final String LOANS_RECEIVABLE_CODE = "1100";
    private static final String GUARANTOR_RECEIVABLE_CODE = "1200";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRepaymentMade(RepaymentMadeEvent event) {
        if (!ledgerEntryRepository.findByTransactionId(event.getEventId()).isEmpty()) {
            log.info("Ledger entries already posted for repayment event {}, skipping", event.getEventId());
            return;
        }

        LedgerAccount cashAndBank = requireAccount(CASH_AND_BANK_CODE);
        LedgerAccount creditAccount = requireAccount(
                event.isLiabilityPayment() ? GUARANTOR_RECEIVABLE_CODE : LOANS_RECEIVABLE_CODE);

        String description = (event.isLiabilityPayment()
                ? "Guarantor liability repayment " : "Loan repayment ")
                + event.getRepaymentId() + " from member " + event.getPayerMemberId();

        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId(event.getEventId())
                .account(cashAndBank)
                .entryType(EntryType.DEBIT)
                .amount(event.getAmount())
                .description(description)
                .build());

        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId(event.getEventId())
                .account(creditAccount)
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
