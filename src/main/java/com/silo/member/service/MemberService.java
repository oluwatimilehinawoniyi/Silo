package com.silo.member.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.DuplicateResourceException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.common.ocr.TextExtractor;
import com.silo.common.storage.DocumentStorage;
import com.silo.member.dto.ExtractedKycFields;
import com.silo.member.dto.KycDocumentUploadResponse;
import com.silo.member.dto.MemberKycUpdateRequest;
import com.silo.member.dto.MemberProfileUpdateRequest;
import com.silo.member.dto.MemberRequest;
import com.silo.member.dto.MemberResponse;
import com.silo.member.dto.MemberStatusUpdateRequest;
import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import com.silo.member.event.MemberRegisteredEvent;
import com.silo.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final DocumentStorage documentStorage;
    private final TextExtractor textExtractor;
    private final KycFieldExtractor kycFieldExtractor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
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

        eventPublisher.publishEvent(new MemberRegisteredEvent(newMember.getId()));

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

    public KycDocumentUploadResponse uploadKycDocument(UUID id, MultipartFile file) {
        Member member = findMemberOrThrow(id);

        String url = documentStorage.upload(file);
        member.setIdDocumentRef(url);
        member = memberRepository.save(member);

        ExtractedKycFields extracted = textExtractor.extractText(url)
                .flatMap(kycFieldExtractor::extract)
                .orElse(null);

        return new KycDocumentUploadResponse(toResponse(member), extracted);
    }

    public List<MemberResponse> listPendingKyc() {
        return memberRepository.findByKycStatus(KYCStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
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
