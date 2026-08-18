package com.silo.paymentgateway.service;

import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.contribution.AutoDebitMandateRecorder;
import com.silo.contribution.AutoDebitMandateStatus;
import com.silo.contribution.AutoDebitMandateSummary;
import com.silo.contribution.AutoDebitPeriodicity;
import com.silo.paymentgateway.PaystackAuthorizationLookup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoDebitSetupServiceTest {

    @Mock
    private PaystackAuthorizationLookup paystackAuthorizationLookup;

    @Mock
    private AutoDebitMandateRecorder mandateRecorder;

    @InjectMocks
    private AutoDebitSetupService service;

    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Test
    @DisplayName("setUpOrReplace rejects a member with no captured Paystack authorization")
    void setUpOrReplace_throwsBusinessRuleViolation_whenNoAuthorization() {
        when(paystackAuthorizationLookup.findLatestAuthorizationCode(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setUpOrReplace(MEMBER_ID, new BigDecimal("1000"), AutoDebitPeriodicity.MONTHLY))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("setUpOrReplace passes the resolved authorization code to the mandate recorder")
    void setUpOrReplace_createsMandate_withResolvedAuthorizationCode() {
        when(paystackAuthorizationLookup.findLatestAuthorizationCode(MEMBER_ID)).thenReturn(Optional.of("AUTH_789"));
        AutoDebitMandateSummary expected = new AutoDebitMandateSummary(
                UUID.randomUUID(), new BigDecimal("1000"), AutoDebitPeriodicity.MONTHLY,
                AutoDebitMandateStatus.ACTIVE, LocalDate.now().plusMonths(1), 0, null);
        when(mandateRecorder.createOrReplace(MEMBER_ID, "AUTH_789", new BigDecimal("1000"), AutoDebitPeriodicity.MONTHLY))
                .thenReturn(expected);

        AutoDebitMandateSummary result = service.setUpOrReplace(MEMBER_ID, new BigDecimal("1000"), AutoDebitPeriodicity.MONTHLY);

        assertThat(result).isEqualTo(expected);
    }
}
