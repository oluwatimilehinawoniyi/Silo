package com.silo.auth.service;

import com.silo.auth.OfficerLookup;
import com.silo.auth.entity.Credential;
import com.silo.auth.entity.Role;
import com.silo.auth.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class OfficerLookupService implements OfficerLookup {

    private final CredentialRepository credentialRepository;

    @Override
    public boolean isOfficer(UUID memberId) {
        return credentialRepository.findByMemberId(memberId)
                .map(credential -> credential.getRole() == Role.OFFICER)
                .orElse(false);
    }

    @Override
    public List<UUID> findAllOfficerMemberIds() {
        return credentialRepository.findByRole(Role.OFFICER).stream()
                .map(Credential::getMemberId)
                .toList();
    }
}
