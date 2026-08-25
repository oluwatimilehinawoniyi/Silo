package com.silo.auth.repository;

import com.silo.auth.entity.Credential;
import com.silo.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {

    Optional<Credential> findByMemberId(UUID memberId);

    Optional<Credential> findByRefreshTokenHash(String refreshTokenHash);

    boolean existsByMemberId(UUID memberId);

    List<Credential> findByRole(Role role);
}
