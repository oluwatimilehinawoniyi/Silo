package com.silo.loan.dto;

import java.util.UUID;

public record AvailableGuarantorResponse(UUID memberId, String email, int credibilityScore) {
}
