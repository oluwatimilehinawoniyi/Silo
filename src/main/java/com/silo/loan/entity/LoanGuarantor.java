package com.silo.loan.entity;

import com.silo.loan.enums.GuarantorStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@Table(name = "loan_guarantors")
public class LoanGuarantor {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID loanRequestId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GuarantorStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime invitedAt;
}
