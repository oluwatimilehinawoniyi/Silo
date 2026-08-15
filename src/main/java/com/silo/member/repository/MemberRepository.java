package com.silo.member.repository;

import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Member, UUID> {

    boolean existsByEmail(String email);

    Optional<Member> findByEmail(String email);

    List<Member> findByStatusAndKycStatus(MemberStatus status, KYCStatus kycStatus);

    List<Member> findByKycStatus(KYCStatus kycStatus);
}
