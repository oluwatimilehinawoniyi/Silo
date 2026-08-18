package com.silo.paymentgateway.service;

import com.silo.contribution.AutoDebitMandateLookup;
import com.silo.contribution.AutoDebitMandateRecorder;
import com.silo.contribution.ContributionRecorder;
import com.silo.contribution.DueAutoDebitMandate;
import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Charges every auto-debit mandate due today or earlier. Each mandate is
 * handled independently - one failure never blocks the rest of the batch,
 * matching every other sweep job in the codebase.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoDebitChargeSweepService {

    private final AutoDebitMandateLookup mandateLookup;
    private final AutoDebitMandateRecorder mandateRecorder;
    private final MemberLookup memberLookup;
    private final PaystackChargeClient paystackChargeClient;
    private final ContributionRecorder contributionRecorder;

    public void sweep() {
        for (DueAutoDebitMandate mandate : mandateLookup.findDueMandates(LocalDate.now())) {
            try {
                chargeOne(mandate);
            } catch (Exception ex) {
                log.error("Auto-debit sweep failed unexpectedly for mandate {}: {}", mandate.mandateId(), ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    void chargeOne(DueAutoDebitMandate mandate) {
        Optional<MemberSummary> member = memberLookup.findById(mandate.memberId());
        if (member.isEmpty()) {
            log.warn("Skipping auto-debit mandate {}: member {} not found", mandate.mandateId(), mandate.memberId());
            mandateRecorder.recordChargeFailure(mandate.mandateId(), "Member not found");
            return;
        }

        PaystackChargeResult result = paystackChargeClient.chargeAuthorization(
                mandate.paystackAuthorizationCode(), mandate.amount(), member.get().email());

        if (result.success()) {
            contributionRecorder.recordPaystackContribution(mandate.memberId(), mandate.amount(), result.reference());
            mandateRecorder.recordChargeSuccess(mandate.mandateId());
        } else {
            mandateRecorder.recordChargeFailure(mandate.mandateId(), result.failureReason());
        }
    }
}
