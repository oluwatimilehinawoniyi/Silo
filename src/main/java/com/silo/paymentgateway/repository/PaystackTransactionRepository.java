package com.silo.paymentgateway.repository;

import com.silo.paymentgateway.entity.PaystackTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaystackTransactionRepository extends JpaRepository<PaystackTransaction, Long> {

    boolean existsByPaystackReference(String paystackReference);

    Optional<PaystackTransaction> findByPaystackReference(String paystackReference);
}