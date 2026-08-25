package com.silo.member.dto;

public record KycDocumentUploadResponse(
        MemberResponse member,
        ExtractedKycFields extracted
) {
}
