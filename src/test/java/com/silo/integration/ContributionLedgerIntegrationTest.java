package com.silo.integration;

import com.silo.accounting.entity.LedgerAccount;
import com.silo.accounting.enums.EntryType;
import com.silo.accounting.repository.LedgerAccountRepository;
import com.silo.accounting.repository.LedgerEntryRepository;
import com.silo.contribution.dto.ContributionRequest;
import com.silo.contribution.repository.ContributionRepository;
import com.silo.contribution.service.ContributionService;
import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import com.silo.member.repository.MemberRepository;
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
 * Proves a recorded contribution posts a balanced double entry to the real
 * ledger via ContributionLedgerListener, which only fires AFTER_COMMIT.
 */
@SpringBootTest
@Testcontainers
class ContributionLedgerIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ContributionService contributionService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ContributionRepository contributionRepository;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @AfterEach
    void cleanUp() {
        ledgerEntryRepository.deleteAll();
        contributionRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    void recordingAContributionPostsBalancedCashAndEquityEntries() {
        Member member = memberRepository.save(Member.builder()
                .fullName("Ada Obi")
                .email("ada.obi@example.com")
                .phoneNumber("08010000000")
                .kycStatus(KYCStatus.VERIFIED)
                .status(MemberStatus.ACTIVE)
                .build());
        Member officer = memberRepository.save(Member.builder()
                .fullName("Bola Officer")
                .email("bola.officer@example.com")
                .phoneNumber("08010000001")
                .kycStatus(KYCStatus.VERIFIED)
                .status(MemberStatus.ACTIVE)
                .build());

        BigDecimal amount = new BigDecimal("5000.00");
        contributionService.recordManualContribution(
                new ContributionRequest(member.getId(), amount, "cash-receipt-001"), officer.getId());

        LedgerAccount cash = ledgerAccountRepository.findByCode("1000").orElseThrow();
        LedgerAccount equity = ledgerAccountRepository.findByCode("3000").orElseThrow();

        assertThat(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(cash.getId(), EntryType.DEBIT))
                .isEqualByComparingTo(amount);
        assertThat(ledgerEntryRepository.sumAmountByAccountIdAndEntryType(equity.getId(), EntryType.CREDIT))
                .isEqualByComparingTo(amount);
    }
}
