package com.silo.paymentgateway.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.contribution.AutoDebitMandateRecorder;
import com.silo.contribution.AutoDebitMandateSummary;
import com.silo.contribution.AutoDebitPeriodicity;
import com.silo.paymentgateway.PaystackAuthorizationLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutoDebitSetupService {

    private final PaystackAuthorizationLookup paystackAuthorizationLookup;
    private final AutoDebitMandateRecorder mandateRecorder;

    public AutoDebitMandateSummary setUpOrReplace(UUID memberId, BigDecimal amount, AutoDebitPeriodicity periodicity) {
        String authorizationCode = paystackAuthorizationLookup.findLatestAuthorizationCode(memberId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Make a Paystack contribution first, then enable auto-debit using that card"));

        return mandateRecorder.createOrReplace(memberId, authorizationCode, amount, periodicity);
    }
}
