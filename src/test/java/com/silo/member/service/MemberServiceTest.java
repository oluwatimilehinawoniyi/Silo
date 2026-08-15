package com.silo.member.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.member.dto.MemberKycUpdateRequest;
import com.silo.member.dto.MemberStatusUpdateRequest;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import com.silo.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    private static final UUID OFFICER_ID = UUID.randomUUID();

    @Test
    @DisplayName("updateStatus rejects an officer changing their own status")
    void updateStatus_throwsBusinessRuleViolation_whenOfficerActsOnSelf() {
        assertThatThrownBy(() -> memberService.updateStatus(
                OFFICER_ID, new MemberStatusUpdateRequest(MemberStatus.SUSPENDED), OFFICER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateKycStatus rejects an officer changing their own KYC status")
    void updateKycStatus_throwsBusinessRuleViolation_whenOfficerActsOnSelf() {
        assertThatThrownBy(() -> memberService.updateKycStatus(
                OFFICER_ID, new MemberKycUpdateRequest(KYCStatus.VERIFIED), OFFICER_ID))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(memberRepository, never()).save(any());
    }
}
