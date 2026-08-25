package com.silo.loan.entity;

import com.silo.loan.enums.RiskTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "borrower_risk_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowerRiskProfile {

    @Id
    private UUID memberId;

    @Column(nullable = false)
    private int totalLoans;

    @Column(nullable = false)
    private int defaultedLoans;

    @Column(nullable = false)
    private int lateLoanPayments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskTier currentRiskTier;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
