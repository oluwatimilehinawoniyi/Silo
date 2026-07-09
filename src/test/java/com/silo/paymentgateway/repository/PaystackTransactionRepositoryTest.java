package com.silo.paymentgateway.repository;

import com.silo.paymentgateway.entity.PaystackTransaction;
import com.silo.paymentgateway.entity.PaystackTransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration-level proof that the migration itself is correct: the unique constraint
 * on paystack_reference really exists in Postgres, and both lookup methods work against
 * the real table. Requires Docker running locally - Spring Boot's docker-compose support
 * starts the Postgres service declared in compose.yaml automatically.
 * <p>
 * member_id is left null here on purpose: it now carries a real FK to members, and this
 * test isn't concerned with member referential integrity, only the dedup constraint.
 * <p>
 * Each test runs inside a transaction that's rolled back afterward, so no manual cleanup
 * is needed between runs.
 */
@SpringBootTest
@Transactional
class PaystackTransactionRepositoryTest {

    @Autowired
    private PaystackTransactionRepository repository;

    @Test
    @DisplayName("paystack_reference is unique at the database level, not just in application code")
    void paystackReference_isUniqueAtTheDatabaseLevel() {
        PaystackTransaction first = repository.saveAndFlush(newTransaction("DUPLICATE_REF"));
        assertThat(first.getId()).isNotNull();

        assertThatThrownBy(() -> repository.saveAndFlush(newTransaction("DUPLICATE_REF")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("existsByPaystackReference and findByPaystackReference find a saved transaction")
    void existsAndFind_returnTheSavedTransaction() {
        repository.saveAndFlush(newTransaction("FINDABLE_REF"));

        assertThat(repository.existsByPaystackReference("FINDABLE_REF")).isTrue();
        assertThat(repository.findByPaystackReference("FINDABLE_REF"))
                .isPresent()
                .get()
                .satisfies(found -> assertThat(found.getStatus()).isEqualTo(PaystackTransactionStatus.RECEIVED));
    }

    @Test
    @DisplayName("existsByPaystackReference is false for a reference never recorded")
    void exists_isFalse_forUnseenReference() {
        assertThat(repository.existsByPaystackReference("NEVER_SEEN_REF")).isFalse();
    }

    private PaystackTransaction newTransaction(String reference) {
        return PaystackTransaction.builder()
                .paystackReference(reference)
                .amount(new BigDecimal("2500.00"))
                .status(PaystackTransactionStatus.RECEIVED)
                .build();
    }
}