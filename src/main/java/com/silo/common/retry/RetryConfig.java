package com.silo.common.retry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Reusable retry policy for outbound calls (Paystack verification, email
 * sending) - exponential backoff with jitter so retries from many failures
 * at once don't all land on the remote service at the same moment. No
 * outbound HTTP caller exists yet (T45, T43); this is the shared policy
 * those will apply once built, either via this RetryTemplate or
 * {@code @Retryable(backoff = @Backoff(..., random = true))} now that
 * {@code @EnableRetry} is active project-wide.
 */
@Configuration
@EnableRetry
public class RetryConfig {

    @Bean
    public RetryTemplate outboundCallRetryTemplate(
            @Value("${silo.retry.max-attempts:3}") int maxAttempts,
            @Value("${silo.retry.initial-interval-ms:500}") long initialIntervalMs,
            @Value("${silo.retry.max-interval-ms:10000}") long maxIntervalMs,
            @Value("${silo.retry.multiplier:2.0}") double multiplier) {
        ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy();
        backOffPolicy.setInitialInterval(initialIntervalMs);
        backOffPolicy.setMaxInterval(maxIntervalMs);
        backOffPolicy.setMultiplier(multiplier);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }
}
