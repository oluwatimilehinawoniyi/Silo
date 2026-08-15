package com.silo.paymentgateway.service;

import com.silo.paymentgateway.repository.PaystackTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaystackReconciliationServiceTest {

    @Mock
    private PaystackApiClient paystackApiClient;

    @Mock
    private PaystackTransactionRepository paystackTransactionRepository;

    @Mock
    private PaystackTransactionService paystackTransactionService;

    private PaystackReconciliationService reconciliationService;

    private static final String REFERENCE = "PAYSTACK_REF_999";
    private static final BigDecimal AMOUNT = new BigDecimal("1000.00");
    private static final String EMAIL = "member@example.com";

    @BeforeEach
    void setUp() {
        reconciliationService = new PaystackReconciliationService(
                paystackApiClient, paystackTransactionRepository, paystackTransactionService, 48L);
    }

    @Test
    @DisplayName("reconcile skips transactions that already have a local record")
    void reconcile_skipsKnownTransactions() {
        PaystackApiTransaction remote = new PaystackApiTransaction(REFERENCE, AMOUNT, EMAIL, Instant.now());
        when(paystackApiClient.listSuccessfulTransactionsSince(any())).thenReturn(List.of(remote));
        when(paystackTransactionRepository.existsByPaystackReference(REFERENCE)).thenReturn(true);

        int recovered = reconciliationService.reconcile();

        assertThat(recovered).isZero();
        verify(paystackTransactionService, never()).processVerifiedTransaction(any(), any(), any());
    }

    @Test
    @DisplayName("reconcile processes a transaction Paystack has but we don't")
    void reconcile_processesMissingTransaction() {
        PaystackApiTransaction remote = new PaystackApiTransaction(REFERENCE, AMOUNT, EMAIL, Instant.now());
        when(paystackApiClient.listSuccessfulTransactionsSince(any())).thenReturn(List.of(remote));
        when(paystackTransactionRepository.existsByPaystackReference(REFERENCE)).thenReturn(false);
        when(paystackTransactionService.processVerifiedTransaction(REFERENCE, AMOUNT, EMAIL)).thenReturn(true);

        int recovered = reconciliationService.reconcile();

        assertThat(recovered).isEqualTo(1);
        verify(paystackTransactionService).processVerifiedTransaction(REFERENCE, AMOUNT, EMAIL);
    }

    @Test
    @DisplayName("reconcile returns zero when Paystack reports no transactions in the lookback window")
    void reconcile_returnsZero_whenNothingToReconcile() {
        when(paystackApiClient.listSuccessfulTransactionsSince(any())).thenReturn(List.of());

        int recovered = reconciliationService.reconcile();

        assertThat(recovered).isZero();
    }
}
