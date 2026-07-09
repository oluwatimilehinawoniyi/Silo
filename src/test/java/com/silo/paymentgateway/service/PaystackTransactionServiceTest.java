package com.silo.paymentgateway.service;

import com.silo.paymentgateway.entity.PaystackTransaction;
import com.silo.paymentgateway.entity.PaystackTransactionStatus;
import com.silo.paymentgateway.repository.PaystackTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PaystackTransactionServiceTest {

    @Mock
    private PaystackTransactionRepository repository;

    @InjectMocks
    private PaystackTransactionService service;

    private static final String REFERENCE = "PAYSTACK_REF_123";
    private static final BigDecimal AMOUNT = new BigDecimal("5000.00");
    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Test
    @DisplayName("recordIfNew saves and returns the transaction when the reference is unseen")
    void recordIfNew_savesNewTransaction_whenReferenceNotSeenBefore() {
        when(repository.existsByPaystackReference(REFERENCE)).thenReturn(false);
        when(repository.save(any(PaystackTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<PaystackTransaction> result = service.recordIfNew(REFERENCE, AMOUNT, MEMBER_ID);

        assertThat(result).isPresent();
        ArgumentCaptor<PaystackTransaction> captor = ArgumentCaptor.forClass(PaystackTransaction.class);
        verify(repository).save(captor.capture());

        PaystackTransaction saved = captor.getValue();
        assertThat(saved.getPaystackReference()).isEqualTo(REFERENCE);
        assertThat(saved.getAmount()).isEqualTo(AMOUNT);
        assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getStatus()).isEqualTo(PaystackTransactionStatus.RECEIVED);
    }

    @Test
    @DisplayName("recordIfNew returns empty and never saves when the reference already exists")
    void recordIfNew_returnsEmpty_whenReferenceAlreadyExists() {
        when(repository.existsByPaystackReference(REFERENCE)).thenReturn(true);

        Optional<PaystackTransaction> result = service.recordIfNew(REFERENCE, AMOUNT, MEMBER_ID);

        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("recordIfNew returns empty when a concurrent duplicate wins the race past the exists() check")
    void recordIfNew_returnsEmpty_whenSaveThrowsDataIntegrityViolationException() {
        when(repository.existsByPaystackReference(REFERENCE)).thenReturn(false);
        when(repository.save(any(PaystackTransaction.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        Optional<PaystackTransaction> result = service.recordIfNew(REFERENCE, AMOUNT, MEMBER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("markProcessed sets status to PROCESSED and stamps processedAt")
    void markProcessed_setsStatusAndProcessedAt_whenTransactionExists() {
        UUID transactionId = UUID.randomUUID();
        PaystackTransaction transaction = PaystackTransaction.builder()
                .id(transactionId)
                .paystackReference(REFERENCE)
                .amount(AMOUNT)
                .memberId(MEMBER_ID)
                .status(PaystackTransactionStatus.RECEIVED)
                .build();
        when(repository.findById(transactionId)).thenReturn(Optional.of(transaction));

        service.markProcessed(transactionId);

        assertThat(transaction.getStatus()).isEqualTo(PaystackTransactionStatus.PROCESSED);
        assertThat(transaction.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("markProcessed does nothing when the transaction id doesn't exist")
    void markProcessed_doesNothing_whenTransactionNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        service.markProcessed(unknownId);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("markFailed sets status to FAILED")
    void markFailed_setsStatus_whenTransactionExists() {
        UUID transactionId = UUID.randomUUID();
        PaystackTransaction transaction = PaystackTransaction.builder()
                .id(transactionId)
                .paystackReference(REFERENCE)
                .amount(AMOUNT)
                .memberId(MEMBER_ID)
                .status(PaystackTransactionStatus.RECEIVED)
                .build();
        when(repository.findById(transactionId)).thenReturn(Optional.of(transaction));

        service.markFailed(transactionId);

        assertThat(transaction.getStatus()).isEqualTo(PaystackTransactionStatus.FAILED);
    }
}