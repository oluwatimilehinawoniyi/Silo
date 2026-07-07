package com.silo.member.dto;

import com.silo.member.enums.MemberStatus;
import jakarta.validation.constraints.NotNull;

public record MemberStatusUpdateRequest(
        @NotNull(message = "Status is required")
        MemberStatus status
) {
}
