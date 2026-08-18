package com.silo.paymentgateway.service;

import com.silo.paymentgateway.PaystackAuthorizationLookup;
import com.silo.paymentgateway.entity.PaystackTransaction;
import com.silo.paymentgateway.repository.PaystackTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class PaystackAuthorizationLookupService implements PaystackAuthorizationLookup {

    private final PaystackTransactionRepository repository;

    @Override
    public Optional<String> findLatestAuthorizationCode(UUID memberId) {
        return repository.findFirstByMemberIdAndAuthorizationCodeIsNotNullOrderByCreatedAtDesc(memberId)
                .map(PaystackTransaction::getAuthorizationCode);
    }
}
