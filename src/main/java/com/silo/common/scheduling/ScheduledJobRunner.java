package com.silo.common.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@Slf4j
public class ScheduledJobRunner {
    private static final String CORRELATION_ID = "correlationId";

    public void run(ScheduledJobName job, Runnable task) {
        MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
        Instant startedAt = Instant.now();

        log.info("scheduled_job_started",
                kv("jobName", job.name()),
                kv("startedAt", startedAt)
        );

        try {
            task.run();
            log.info(
                    "scheduled_job_completed",
                    kv("jobName", job.name()),
                    kv("status", "SUCCESSFUL"),
                    kv("durationMs", Duration.between(startedAt, Instant.now()).toMillis())
            );
        } catch (Exception ex) {
            log.error(
                    "scheduled_job_failed",
                    kv("jobName", job.name()),
                    kv("status", "FAILED"),
                    kv("durationMs", Duration.between(startedAt, Instant.now()).toMillis()),
                    kv("exception", ex.getClass().getSimpleName()),
                    kv("errorMessage", ex.getMessage()),
                    ex
            );
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }
}
