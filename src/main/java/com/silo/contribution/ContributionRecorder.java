package com.silo.contribution;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The only way another module (Payment Gateway) can cause a Contribution to
 * be recorded. Keeps Contribution the sole writer of its own table and the
 * sole publisher of ContributionMadeEvent, regardless of source.
 */
public interface ContributionRecorder {

    void recordPaystackContribution(UUID memberId, BigDecimal amount, String reference);
}
