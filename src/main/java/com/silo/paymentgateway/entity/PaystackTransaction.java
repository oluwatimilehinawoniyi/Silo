package com.silo.paymentgateway.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Builder
@EqualsAndHashCode(of = "id")
@Table(name = "paystack_transactions")
public class PaystackTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String paystackReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaystackTransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @JoinColumn(name = "member_id", nullable = false)
    private UUID memberId;

    @CreationTimestamp
    @Column( nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant processedAt;

}