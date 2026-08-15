package com.silo.loan.service;

import com.silo.loan.LoanLookup;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class LoanLookupService implements LoanLookup {

    private final LoanRepository loanRepository;

    @Override
    public boolean exists(UUID loanId) {
        return loanRepository.existsById(loanId);
    }

    @Override
    public boolean isActive(UUID loanId) {
        return loanRepository.findById(loanId)
                .map(loan -> loan.getStatus() == LoanStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    public boolean belongsToMember(UUID loanId, UUID memberId) {
        return loanRepository.findById(loanId)
                .map(loan -> loan.getMemberId().equals(memberId))
                .orElse(false);
    }
}
