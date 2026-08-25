package com.silo.accounting.service;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.entity.LedgerEntry;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.loan.event.GuarantorLiabilityAllocation;
import com.silo.loan.event.LoanDefaultedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class LoanDefaultLedgerListener {

    private static final String LOANS_RECEIVABLE_CODE = "1100";
    private static final String GUARANTOR_RECEIVABLE_CODE = "1200";
    private static final String PENALTY_RECEIVABLE_CODE = "1300";
    private static final String PENALTY_INCOME_CODE = "4100";

    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BigDecimal penaltyRate;

    public LoanDefaultLedgerListener(
            LedgerAccountRepository ledgerAccountRepository,
            LedgerEntryRepository ledgerEntryRepository,
            @Value("${silo.loan.default-penalty-rate:0.05}") BigDecimal penaltyRate) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.penaltyRate = penaltyRate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLoanDefaulted(LoanDefaultedEvent event) {
        if (!ledgerEntryRepository.findByTransactionId(event.getEventId()).isEmpty()) {
            log.info("Ledger entries already posted for loan default event {}, skipping", event.getEventId());
            return;
        }

        LedgerAccount guarantorReceivable = requireAccount(GUARANTOR_RECEIVABLE_CODE);
        LedgerAccount loansReceivable = requireAccount(LOANS_RECEIVABLE_CODE);

        for (GuarantorLiabilityAllocation allocation : event.getGuarantorLiabilityAllocations()) {
            String description = "Guarantor liability " + allocation.getLiabilityId()
                    + " transfer for defaulted loan " + event.getLoanId();

            ledgerEntryRepository.save(LedgerEntry.builder()
                    .transactionId(event.getEventId())
                    .account(guarantorReceivable)
                    .entryType(EntryType.DEBIT)
                    .amount(allocation.getAmount())
                    .description(description)
                    .build());

            ledgerEntryRepository.save(LedgerEntry.builder()
                    .transactionId(event.getEventId())
                    .account(loansReceivable)
                    .entryType(EntryType.CREDIT)
                    .amount(allocation.getAmount())
                    .description(description)
                    .build());
        }

        postPenalty(event);
    }

    private void postPenalty(LoanDefaultedEvent event) {
        BigDecimal penaltyAmount = event.getOutstandingBalance()
                .multiply(penaltyRate)
                .setScale(2, RoundingMode.HALF_UP);

        LedgerAccount penaltyReceivable = requireAccount(PENALTY_RECEIVABLE_CODE);
        LedgerAccount penaltyIncome = requireAccount(PENALTY_INCOME_CODE);
        String description = "Default penalty for loan " + event.getLoanId() + ", member " + event.getMemberId();

        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId(event.getEventId())
                .account(penaltyReceivable)
                .entryType(EntryType.DEBIT)
                .amount(penaltyAmount)
                .description(description)
                .build());

        ledgerEntryRepository.save(LedgerEntry.builder()
                .transactionId(event.getEventId())
                .account(penaltyIncome)
                .entryType(EntryType.CREDIT)
                .amount(penaltyAmount)
                .description(description)
                .build());
    }

    private LedgerAccount requireAccount(String code) {
        return ledgerAccountRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Chart of accounts missing account code " + code));
    }
}
