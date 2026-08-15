package com.silo.member.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.storage.DocumentStorage;
import com.silo.member.dto.MemberKycUpdateRequest;
import com.silo.member.dto.MemberRequest;
import com.silo.member.dto.MemberResponse;
import com.silo.member.dto.MemberStatusUpdateRequest;
import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import com.silo.member.event.MemberRegisteredEvent;
import com.silo.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private DocumentStorage documentStorage;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberService memberService;

    private static final UUID OFFICER_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();

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

    @Test
    @DisplayName("createMember publishes MemberRegisteredEvent")
    void createMember_publishesMemberRegisteredEvent() {
        when(memberRepository.existsByEmail("chidi@example.com")).thenReturn(false);
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setId(MEMBER_ID);
            return member;
        });

        memberService.createMember(new MemberRequest("Chidi Eze", "chidi@example.com", "08010000000"));

        ArgumentCaptor<MemberRegisteredEvent> captor = ArgumentCaptor.forClass(MemberRegisteredEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    @DisplayName("uploadKycDocument stores the file and sets idDocumentRef to the returned URL")
    void uploadKycDocument_setsIdDocumentRef() {
        Member member = Member.builder()
                .id(MEMBER_ID).fullName("Chidi Eze").email("chidi@example.com").phoneNumber("08010000000")
                .kycStatus(KYCStatus.PENDING).status(MemberStatus.ACTIVE).build();
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "id-card.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(documentStorage.upload(file)).thenReturn("https://res.cloudinary.com/silo/id-card.jpg");

        MemberResponse response = memberService.uploadKycDocument(MEMBER_ID, file);

        assertThat(response.idDocumentRef()).isEqualTo("https://res.cloudinary.com/silo/id-card.jpg");
    }

    @Test
    @DisplayName("listPendingKyc returns members awaiting KYC review")
    void listPendingKyc_returnsPendingMembers() {
        Member member = Member.builder()
                .id(MEMBER_ID).fullName("Chidi Eze").email("chidi@example.com").phoneNumber("08010000000")
                .kycStatus(KYCStatus.PENDING).status(MemberStatus.ACTIVE).build();
        when(memberRepository.findByKycStatus(KYCStatus.PENDING)).thenReturn(List.of(member));

        List<MemberResponse> pending = memberService.listPendingKyc();

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).id()).isEqualTo(MEMBER_ID);
    }
}
