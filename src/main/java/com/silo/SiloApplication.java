package com.silo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Spring Modulith's own JPA-backed event publication registry is disabled -
// this project's transactional outbox (event_outbox, see com.silo.common.outbox)
// is the single mechanism for durably recording and redelivering domain events.
// excludeName (not exclude) because these autoconfigurations ship in
// runtime-scoped Modulith jars, unavailable on the compile classpath.
@SpringBootApplication(excludeName = {
        "org.springframework.modulith.events.config.EventPublicationAutoConfiguration",
        "org.springframework.modulith.events.jpa.JpaEventPublicationAutoConfiguration"
})
@EnableScheduling
public class SiloApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiloApplication.class, args);
    }

}
