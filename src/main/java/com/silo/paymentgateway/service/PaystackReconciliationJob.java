package com.silo.paymentgateway.service;

import com.silo.common.scheduling.ScheduledJobName;
import com.silo.common.scheduling.ScheduledJobRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaystackReconciliationJob {

    private final ScheduledJobRunner scheduledJobRunner;
    private final PaystackReconciliationService paystackReconciliationService;

    @Scheduled(cron = "${silo.jobs.paystack-reconciliation-cron:0 30 1 * * *}")
    public void run() {
        scheduledJobRunner.run(ScheduledJobName.PAYSTACK_RECONCILIATION_SWEEP, paystackReconciliationService::reconcile);
    }
}
