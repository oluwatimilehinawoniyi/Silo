package com.silo.loan.entity;

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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "guarantor_credibility_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuarantorCredibilityProfile {

    @Id
    private UUID memberId;

    @Column(nullable = false)
    private int timesGuaranteed;

    @Column(nullable = false)
    private int loansWentBad;

    @Column(nullable = false)
    private int successfulGuarantees;

    @Column(nullable = false)
    private int credibilityScore;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
