package com.silo.contribution.entity;

import com.silo.contribution.AutoDebitMandateStatus;
import com.silo.contribution.AutoDebitPeriodicity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contribution_auto_debit_mandates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionAutoDebitMandate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, length = 120)
    private String paystackAuthorizationCode;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AutoDebitPeriodicity periodicity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AutoDebitMandateStatus status;

    @Column(nullable = false)
    private LocalDate nextChargeDate;

    @Builder.Default
    @Column(nullable = false)
    private int consecutiveFailureCount = 0;

    @Column(length = 500)
    private String lastFailureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
