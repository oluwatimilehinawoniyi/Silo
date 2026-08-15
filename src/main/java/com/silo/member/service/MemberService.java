package com.silo.member.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.DuplicateResourceException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.member.dto.MemberKycUpdateRequest;
import com.silo.member.dto.MemberProfileUpdateRequest;
import com.silo.member.dto.MemberRequest;
import com.silo.member.dto.MemberResponse;
import com.silo.member.dto.MemberStatusUpdateRequest;
import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import com.silo.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberResponse createMember(MemberRequest request) {

        boolean emailExists =
                memberRepository.existsByEmail(request.email());

        // verify email is unique
        if (emailExists) {
            throw new DuplicateResourceException(
                    "Member with email " + request.email() + " already exists");
        }

        Member newMember = Member
                .builder()
                .fullName(request.fullName())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .kycStatus(KYCStatus.PENDING)
                .status(MemberStatus.ACTIVE)
                .build();

        newMember = memberRepository.save(newMember);
        return toResponse(newMember);
    }

    public MemberResponse getMember(UUID id) {
        return toResponse(findMemberOrThrow(id));
    }

    public MemberResponse updateProfile(
            UUID id,
            MemberProfileUpdateRequest request) {
        Member member = findMemberOrThrow(id);

        member.setFullName(request.fullName());
        member.setPhoneNumber(request.phoneNumber());
        member.setIdType(request.idType());
        member.setIdNumber(request.idNumber());
        member.setIdDocumentRef(request.idDocumentRef());

        member = memberRepository.save(member);
        return toResponse(member);
    }

    public MemberResponse updateStatus(
            UUID id,
            MemberStatusUpdateRequest request,
            UUID actingOfficerId) {
        if (id.equals(actingOfficerId)) {
            throw new BusinessRuleViolationException("An officer cannot change their own status");
        }
        Member member = findMemberOrThrow(id);

        member.setStatus(request.status());

        member = memberRepository.save(member);
        return toResponse(member);
    }

    public MemberResponse updateKycStatus(
            UUID id,
            MemberKycUpdateRequest request,
            UUID actingOfficerId) {
        if (id.equals(actingOfficerId)) {
            throw new BusinessRuleViolationException("An officer cannot change their own KYC status");
        }
        Member member = findMemberOrThrow(id);

        if (request.kycStatus() == KYCStatus.PENDING) {
            throw new BusinessRuleViolationException(
                    "KYC status cannot be reverted to PENDING");
        }

        member.setKycStatus(request.kycStatus());

        member = memberRepository.save(member);
        return toResponse(member);
    }

    private Member findMemberOrThrow(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member not found with id " + id));
    }

    // helper function to turn member to response
    private MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getFullName(),
                member.getEmail(),
                member.getPhoneNumber(),
                member.getKycStatus(),
                member.getIdType(),
                member.getIdNumber(),
                member.getIdDocumentRef(),
                member.getStatus(),
                member.getJoinedDate()
        );
    }
}
