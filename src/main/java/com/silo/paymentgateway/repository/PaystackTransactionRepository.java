package com.silo.paymentgateway.repository;

import com.silo.paymentgateway.entity.PaystackTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaystackTransactionRepository extends JpaRepository<PaystackTransaction, UUID> {

    boolean existsByPaystackReference(String paystackReference);

    Optional<PaystackTransaction> findByPaystackReference(String paystackReference);
}