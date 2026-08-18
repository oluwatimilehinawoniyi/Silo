package com.silo.paymentgateway.service;

import com.silo.common.scheduling.ScheduledJobName;
import com.silo.common.scheduling.ScheduledJobRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoDebitChargeSweepJob {

    private final ScheduledJobRunner scheduledJobRunner;
    private final AutoDebitChargeSweepService autoDebitChargeSweepService;

    @Scheduled(cron = "${silo.jobs.auto-debit-sweep-cron:0 0 2 * * *}")
    public void run() {
        scheduledJobRunner.run(ScheduledJobName.AUTO_DEBIT_CHARGE_SWEEP, autoDebitChargeSweepService::sweep);
    }
}
