package com.silo.member.service;

import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import com.silo.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public Optional<MemberSummary> findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .map(member -> new MemberSummary(member.getId(), member.getEmail()));
    }
}
