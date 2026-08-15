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
@Table(name = "reporting_member_summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportingMemberSummary {

    @Id
    private UUID memberId;

    @Column(nullable = false)
    private BigDecimal totalContributions;

    @Column(nullable = false)
    private int activeLoans;

    @Column(nullable = false)
    private BigDecimal totalRepayments;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
