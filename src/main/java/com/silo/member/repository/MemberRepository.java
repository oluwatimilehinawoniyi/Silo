package com.silo.member.repository;

import com.silo.member.entity.Member;
import com.silo.member.enums.KYCStatus;
import com.silo.member.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("select m from Member m where "
            + "(:kycStatus is null or m.kycStatus = :kycStatus) and "
            + "(:search is null or lower(m.fullName) like lower(concat('%', cast(:search as string), '%')) "
            + "or lower(m.email) like lower(concat('%', cast(:search as string), '%')))")
    List<Member> search(@Param("kycStatus") KYCStatus kycStatus, @Param("search") String search);
}
