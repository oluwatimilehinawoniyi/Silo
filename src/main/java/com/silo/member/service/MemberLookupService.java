package com.silo.member.service;

import com.silo.member.MemberLookup;
import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import com.silo.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class MemberLookupService implements MemberLookup {

    private final MemberRepository memberRepository;

    @Override
    public boolean exists(UUID memberId) {
        return memberRepository.existsById(memberId);
    }

    @Override
    public boolean isActiveAndVerified(UUID memberId) {
        return memberRepository.findById(memberId)
                .map(this::isActiveAndVerified)
                .orElse(false);
    }

    private boolean isActiveAndVerified(Member member) {
        return member.getStatus() == MemberStatus.ACTIVE
                && member.getKycStatus() == KYCStatus.VERIFIED;
    }
}
