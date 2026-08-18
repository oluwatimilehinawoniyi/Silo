package com.silo.contribution;

import java.math.BigDecimal;
import java.util.UUID;

public interface AutoDebitMandateRecorder {

    /**
     * Creates a mandate using an authorization code the caller has already
     * resolved - contribution never looks this up itself, since doing so
     * would mean depending on the paymentgateway module, which depends
     * back on contribution for the charge sweep. Replaces (cancels) any
     * existing non-cancelled mandate for the member first.
     */
    AutoDebitMandateSummary createOrReplace(
            UUID memberId, String paystackAuthorizationCode, BigDecimal amount, AutoDebitPeriodicity periodicity);

    /** Advances the mandate to its next charge date and clears any failure streak. */
    void recordChargeSuccess(UUID mandateId);

    /**
     * Records one failed attempt. Once consecutive failures cross the
     * threshold the mandate stops (status FAILED, no further attempts) -
     * either way this fires a notification so the member hears about it.
     */
    void recordChargeFailure(UUID mandateId, String reason);
}
