package com.silo.reporting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reporting_loan_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportingLoanSummary {

    @Id
    private UUID loanId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private BigDecimal outstandingBalance;

    @Column(nullable = false)
    private int daysOverdue;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
