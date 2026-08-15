package com.silo.member.service;

import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import com.silo.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
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

    @Override
    public Optional<MemberSummary> findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .map(member -> new MemberSummary(member.getId(), member.getEmail()));
    }

    @Override
    public Optional<MemberSummary> findById(UUID memberId) {
        return memberRepository.findById(memberId)
                .map(member -> new MemberSummary(member.getId(), member.getEmail()));
    }

    @Override
    public List<MemberSummary> findAllActiveAndVerified() {
        return memberRepository.findByStatusAndKycStatus(MemberStatus.ACTIVE, KYCStatus.VERIFIED)
                .stream()
                .map(member -> new MemberSummary(member.getId(), member.getEmail()))
                .toList();
    }

    private boolean isActiveAndVerified(Member member) {
        return member.getStatus() == MemberStatus.ACTIVE
                && member.getKycStatus() == KYCStatus.VERIFIED;
    }
}