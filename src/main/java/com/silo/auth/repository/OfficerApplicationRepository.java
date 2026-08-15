package com.silo.auth.repository;

import com.silo.auth.entity.OfficerApplication;
import com.silo.auth.entity.OfficerApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfficerApplicationRepository extends JpaRepository<OfficerApplication, UUID> {

    boolean existsByMemberIdAndStatus(UUID memberId, OfficerApplicationStatus status);

    List<OfficerApplication> findByStatus(OfficerApplicationStatus status);

    List<OfficerApplication> findByMemberIdOrderByCreatedAtDesc(UUID memberId);
}
