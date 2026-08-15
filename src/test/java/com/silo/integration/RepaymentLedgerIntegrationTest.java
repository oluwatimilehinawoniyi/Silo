package com.silo.integration;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.enums.LoanRequestStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.repository.LoanRepository;
import com.silo.loan.repository.LoanRequestRepository;
import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import com.silo.member.repository.MemberRepository;
import com.silo.notification.repository.NotificationLogRepository;
import com.silo.repayment.dto.RepaymentRequest;
import com.silo.repayment.repository.RepaymentRepository;
import com.silo.repayment.service.RepaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves a borrower repayment against an ACTIVE loan posts a Cash-for-
 * Loans-Receivable ledger entry via RepaymentLedgerListener, which only
 * fires AFTER_COMMIT.
 * <p>
 * Note: there is no loan/installment closure logic in this codebase yet
 * (nothing sets LoanStatus.CLOSED or advances installment status on
 * repayment), so this only covers the ledger side of the flow.
 */
@SpringBootTest
@Testcontainers
class RepaymentLedgerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RepaymentService repaymentService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRequestRepository loanRequestRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private RepaymentRepository repaymentRepository;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @AfterEach
    void cleanUp() {
        ledgerEntryRepository.deleteAll();
        notificationLogRepository.deleteAll();
        repaymentRepository.deleteAll();
        loanRepository.deleteAll();
        loanRequestRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void recordingABorrowerRepaymentPostsCashAgainstLoansReceivable() {
        Member borrower = memberRepository.save(Member.builder()
                .fullName("Chidi Eze")
                .email("chidi.eze@example.com")
                .phoneNumber("08010000002")
                .kycStatus(KYCStatus.VERIFIED)
                .status(MemberStatus.ACTIVE)
                .build());

        LoanRequest loanRequest = loanRequestRepository.save(LoanRequest.builder()
                .memberId(borrower.getId())
                .amountRequested(new BigDecimal("6000.00"))
                .purpose("School fees")
                .status(LoanRequestStatus.APPROVED)
                .build());

        Loan loan = loanRepository.save(Loan.builder()
                .loanRequestId(loanRequest.getId())
                .memberId(borrower.getId())
                .principalAmount(new BigDecimal("6000.00"))
                .interestRate(new BigDecimal("0.10"))
                .durationMonths(6)
                .status(LoanStatus.ACTIVE)
                .outstandingBalance(new BigDecimal("6000.00"))
                .build());

        BigDecimal repaymentAmount = new BigDecimal("1000.00");
        repaymentService.recordBorrowerRepayment(borrower.getId(),
                new RepaymentRequest(loan.getId(), repaymentAmount, "repayment-001"));

        LedgerAccount cash = ledgerAccountRepository.findByCode("1000").orElseThrow();
        LedgerAccount loansReceivable = ledgerAccountRepository.findByCode("1100").orElseThrow();

        assertThat(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(cash.getId(), EntryType.DEBIT))
                .isEqualByComparingTo(repaymentAmount);
        assertThat(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(loansReceivable.getId(), EntryType.CREDIT))
                .isEqualByComparingTo(repaymentAmount);
    }
}
