package com.silo.member.dto;

public record ExtractedKycFields(
        String idType,
        String idNumber,
        double confidence
) {
}
