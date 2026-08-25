package com.silo.auth.repository;

import com.silo.auth.entity.OfficerApplicationApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfficerApplicationApprovalRepository extends JpaRepository<OfficerApplicationApproval, UUID> {

    boolean existsByApplicationIdAndOfficerId(UUID applicationId, UUID officerId);

    List<OfficerApplicationApproval> findByApplicationId(UUID applicationId);

    long countByApplicationId(UUID applicationId);
}
