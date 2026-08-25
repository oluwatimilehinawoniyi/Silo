package com.silo.loan.service;

import com.silo.loan.GuarantorLiabilityLookup;
import com.silo.loan.enums.LiabilityStatus;
import com.silo.loan.repository.GuarantorLiabilityRepository;
import com.silo.loan.entity.GuarantorLiability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class GuarantorLiabilityLookupService implements GuarantorLiabilityLookup {

    private final GuarantorLiabilityRepository guarantorLiabilityRepository;

    @Override
    public boolean exists(UUID liabilityId) {
        return guarantorLiabilityRepository.existsById(liabilityId);
    }

    @Override
    public boolean isPending(UUID liabilityId) {
        return guarantorLiabilityRepository.findById(liabilityId)
                .map(liability -> liability.getStatus() == LiabilityStatus.PENDING)
                .orElse(false);
    }

    @Override
    public boolean belongsToGuarantor(UUID liabilityId, UUID guarantorMemberId) {
        return guarantorLiabilityRepository.findById(liabilityId)
                .map(liability -> liability.getGuarantorMemberId().equals(guarantorMemberId))
                .orElse(false);
    }

    @Override
    public Optional<UUID> findLoanId(UUID liabilityId) {
        return guarantorLiabilityRepository.findById(liabilityId).map(GuarantorLiability::getLoanId);
    }
}
