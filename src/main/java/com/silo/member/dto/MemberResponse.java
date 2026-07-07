package com.silo.member.dto;

import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        KYCStatus kycStatus,
        String idType,
        String idNumber,
        String idDocumentRef,
        MemberStatus status,
        LocalDateTime joinedDate
) {
}
