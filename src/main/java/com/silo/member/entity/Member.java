package com.silo.member.entity;

import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private KYCStatus kycStatus;

    @Column
    private String idType;
    @Column
    private String idNumber;
    @Column
    private String idDocumentRef;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedDate;
}
