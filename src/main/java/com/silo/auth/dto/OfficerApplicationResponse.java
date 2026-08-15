package com.silo.auth.dto;

import com.silo.auth.entity.OfficerApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OfficerApplicationResponse(
        UUID id,
        UUID memberId,
        OfficerApplicationStatus status,
        int approvalCount,
        boolean approvedByCaller,
        LocalDateTime createdAt,
        LocalDateTime decidedAt
) {
}
