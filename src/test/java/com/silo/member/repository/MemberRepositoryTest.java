package com.silo.member.repository;

import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration-level proof that search() actually runs against Postgres: a null search
 * parameter previously made the JDBC driver infer its type as bytea, so lower(bytea)
 * blew up at runtime even though the JPQL compiled fine. A Mockito unit test can't
 * catch that class of bug since the repository itself is mocked out.
 */
@SpringBootTest
@Testcontainers
@Transactional
class MemberRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("search runs with a null search term and a null kycStatus, without SQL type errors")
    void search_runsWithBothFiltersNull() {
        memberRepository.saveAndFlush(newMember("Jane Doe", "jane@example.com"));

        List<Member> results = memberRepository.search(null, null);

        assertThat(results).extracting(Member::getEmail).contains("jane@example.com");
    }

    @Test
    @DisplayName("search matches on a case-insensitive substring of fullName or email")
    void search_matchesCaseInsensitiveSubstring() {
        memberRepository.saveAndFlush(newMember("Jane Doe", "jane@example.com"));
        memberRepository.saveAndFlush(newMember("John Smith", "john@example.com"));

        List<Member> results = memberRepository.search(null, "JANE");

        assertThat(results).extracting(Member::getEmail).containsExactly("jane@example.com");
    }

    @Test
    @DisplayName("search filters by kycStatus when a search term is also null")
    void search_filtersByKycStatusOnly() {
        memberRepository.saveAndFlush(newMember("Jane Doe", "jane@example.com"));

        List<Member> results = memberRepository.search(KYCStatus.PENDING, null);

        assertThat(results).extracting(Member::getEmail).contains("jane@example.com");
    }

    private Member newMember(String fullName, String email) {
        return Member.builder()
                .fullName(fullName)
                .email(email)
                .phoneNumber("08012345678")
                .kycStatus(KYCStatus.PENDING)
                .status(MemberStatus.ACTIVE)
                .build();
    }
}
