package com.silo.common.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("scheduled-job-");
        // Belt-and-braces: ScheduledJobRunner already catches Exception around each
        // job body, but this catches anything that escapes outside that (e.g. a job
        // wired up without going through the runner, or an Error rather than Exception).
        scheduler.setErrorHandler(throwable ->
                log.error("Uncaught exception escaped a scheduled job", throwable));
        scheduler.initialize();
        return scheduler;
    }
}
