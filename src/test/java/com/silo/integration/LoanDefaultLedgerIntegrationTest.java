package com.silo.integration;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanGuarantor;
import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.entity.LoanRequest;
import com.silo.loan.entity.GuarantorLiability;
import com.silo.loan.enums.GuarantorStatus;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.enums.LiabilityStatus;
import com.silo.loan.enums.LoanRequestStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.repository.BorrowerRiskProfileRepository;
import com.silo.loan.repository.GuarantorCredibilityProfileRepository;
import com.silo.loan.repository.GuarantorLiabilityRepository;
import com.silo.loan.repository.LoanGuarantorRepository;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import com.silo.loan.repository.LoanRequestRepository;
import com.silo.loan.service.DefaultDetectionService;
import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import com.silo.member.repository.MemberRepository;
import com.silo.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves a loan with three consecutive LATE installments gets caught by
 * DefaultDetectionService, which assigns guarantor liability and publishes
 * LoanDefaultedEvent, and that LoanDefaultLedgerListener posts the
 * guarantor-receivable transfer plus the penalty entries AFTER_COMMIT.
 */
@SpringBootTest
@Testcontainers
class LoanDefaultLedgerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DefaultDetectionService defaultDetectionService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRequestRepository loanRequestRepository;

    @Autowired
    private LoanGuarantorRepository loanGuarantorRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanInstallmentRepository loanInstallmentRepository;

    @Autowired
    private GuarantorLiabilityRepository guarantorLiabilityRepository;

    @Autowired
    private BorrowerRiskProfileRepository borrowerRiskProfileRepository;

    @Autowired
    private GuarantorCredibilityProfileRepository guarantorCredibilityProfileRepository;

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
        guarantorLiabilityRepository.deleteAll();
        borrowerRiskProfileRepository.deleteAll();
        guarantorCredibilityProfileRepository.deleteAll();
        loanInstallmentRepository.deleteAll();
        loanRepository.deleteAll();
        loanGuarantorRepository.deleteAll();
        loanRequestRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void threeConsecutiveLateInstallmentsTriggerDefaultLiabilityAndLedgerPosting() {
        Member borrower = memberRepository.save(newMember("borrower@example.com"));
        Member guarantorMember = memberRepository.save(newMember("guarantor@example.com"));

        LoanRequest loanRequest = loanRequestRepository.save(LoanRequest.builder()
                .memberId(borrower.getId())
                .amountRequested(new BigDecimal("9000.00"))
                .purpose("Business expansion")
                .status(LoanRequestStatus.APPROVED)
                .build());

        loanGuarantorRepository.save(LoanGuarantor.builder()
                .loanRequestId(loanRequest.getId())
                .memberId(guarantorMember.getId())
                .status(GuarantorStatus.ACCEPTED)
                .build());

        BigDecimal outstandingBalance = new BigDecimal("9000.00");
        Loan loan = loanRepository.save(Loan.builder()
                .loanRequestId(loanRequest.getId())
                .memberId(borrower.getId())
                .principalAmount(outstandingBalance)
                .interestRate(new BigDecimal("0.10"))
                .durationMonths(6)
                .status(LoanStatus.ACTIVE)
                .outstandingBalance(outstandingBalance)
                .build());

        for (int i = 1; i <= 3; i++) {
            loanInstallmentRepository.save(LoanInstallment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .dueDate(LocalDateTime.now().minusDays(30L * i))
                    .expectedAmount(new BigDecimal("1500.00"))
                    .status(InstallmentStatus.LATE)
                    .build());
        }

        int defaultedCount = defaultDetectionService.detectDefaults();
        assertThat(defaultedCount).isEqualTo(1);

        Loan reloaded = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(LoanStatus.DEFAULTED);

        List<GuarantorLiability> liabilities = guarantorLiabilityRepository.findByLoanId(loan.getId());
        assertThat(liabilities).hasSize(1);
        assertThat(liabilities.get(0).getGuarantorMemberId()).isEqualTo(guarantorMember.getId());
        assertThat(liabilities.get(0).getStatus()).isEqualTo(LiabilityStatus.PENDING);
        assertThat(liabilities.get(0).getAmount()).isEqualByComparingTo(outstandingBalance);

        LedgerAccount guarantorReceivable = ledgerAccountRepository.findByCode("1200").orElseThrow();
        LedgerAccount loansReceivable = ledgerAccountRepository.findByCode("1100").orElseThrow();
        LedgerAccount penaltyReceivable = ledgerAccountRepository.findByCode("1300").orElseThrow();
        LedgerAccount penaltyIncome = ledgerAccountRepository.findByCode("4100").orElseThrow();

        assertThat(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(guarantorReceivable.getId(), EntryType.DEBIT))
                .isEqualByComparingTo(outstandingBalance);
        assertThat(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(loansReceivable.getId(), EntryType.CREDIT))
                .isEqualByComparingTo(outstandingBalance);

        BigDecimal expectedPenalty = outstandingBalance
                .multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_UP);
        assertThat(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(penaltyReceivable.getId(), EntryType.DEBIT))
                .isEqualByComparingTo(expectedPenalty);
        assertThat(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(penaltyIncome.getId(), EntryType.CREDIT))
                .isEqualByComparingTo(expectedPenalty);
    }

    private Member newMember(String email) {
        return Member.builder()
                .fullName("Test Member")
                .email(email)
                .phoneNumber("08010000001")
                .kycStatus(KYCStatus.VERIFIED)
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
