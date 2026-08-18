package com.silo.paymentgateway;

import java.util.Optional;
import java.util.UUID;

public interface PaystackAuthorizationLookup {

    /**
     * @return the reusable authorization code from the member's most recent
     * successful card payment, if their card supports future charges
     */
    Optional<String> findLatestAuthorizationCode(UUID memberId);
}
