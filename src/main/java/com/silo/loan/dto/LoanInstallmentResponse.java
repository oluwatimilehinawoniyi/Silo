package com.silo.loan.dto;

import com.silo.loan.enums.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoanInstallmentResponse(
        UUID id,
        Integer installmentNumber,
        LocalDateTime dueDate,
        BigDecimal expectedAmount,
        InstallmentStatus status,
        LocalDateTime paidDate
) {
}
