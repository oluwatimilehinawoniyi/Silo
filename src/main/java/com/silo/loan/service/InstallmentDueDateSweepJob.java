package com.silo.loan.service;

import com.silo.common.scheduling.ScheduledJobName;
import com.silo.common.scheduling.ScheduledJobRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstallmentDueDateSweepJob {

    private final ScheduledJobRunner scheduledJobRunner;
    private final InstallmentDueDateSweepService installmentDueDateSweepService;

    @Scheduled(cron = "${silo.jobs.installment-sweep-cron:0 0 1 * * *}")
    public void run() {
        scheduledJobRunner.run(
                ScheduledJobName.INSTALLMENT_DUE_DATE_SWEEP,
                installmentDueDateSweepService::sweepOverdueInstallments);
    }
}
