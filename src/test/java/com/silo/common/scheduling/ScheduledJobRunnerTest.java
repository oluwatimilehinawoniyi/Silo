package com.silo.common.scheduling;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduledJobRunnerTest {

    private final ScheduledJobRunner runner = new ScheduledJobRunner();

    @Test
    void should_run_task_to_completion() {
        AtomicBoolean ran = new AtomicBoolean(false);

        runner.run(ScheduledJobName.PAYSTACK_RECONCILIATION_SWEEP, () -> ran.set(true));

        assertTrue(ran.get());
    }

    @Test
    void should_not_throw_when_task_fails() {
        assertDoesNotThrow(() ->
                runner.run(ScheduledJobName.INSTALLMENT_DUE_DATE_SWEEP, () -> {
                    throw new RuntimeException("simulated job failure");
                })
        );
    }

    @Test
    void should_clear_correlation_id_after_success() {
        runner.run(ScheduledJobName.PAYSTACK_RECONCILIATION_SWEEP, () -> {
        });

        assertNull(MDC.get("correlationId"));
    }

    @Test
    void should_clear_correlation_id_after_failure() {
        runner.run(ScheduledJobName.PAYSTACK_RECONCILIATION_SWEEP, () -> {
            throw new IllegalStateException("simulated job failure");
        });

        assertNull(MDC.get("correlationId"));
    }
}