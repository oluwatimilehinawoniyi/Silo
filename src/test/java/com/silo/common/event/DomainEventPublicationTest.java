package com.silo.common.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the ApplicationEventPublisher / @TransactionalEventListener wiring
 * that every module listener (Accounting, Reporting, Notification...) relies
 * on - using an in-memory transaction manager so the assertions don't need a
 * real database.
 */
class DomainEventPublicationTest {

    private final AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(Config.class);

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    @DisplayName("listener receives the event once its publishing transaction commits")
    void listenerReceivesEventAfterCommit() {
        publishWithinTransaction(false);

        assertThat(context.getBean(TestEventListener.class).received).hasSize(1);
    }

    @Test
    @DisplayName("listener never receives the event if its publishing transaction rolls back")
    void listenerSkipsEventOnRollback() {
        publishWithinTransaction(true);

        assertThat(context.getBean(TestEventListener.class).received).isEmpty();
    }

    @Test
    @DisplayName("listener never receives an event published with no active transaction")
    void listenerSkipsEventPublishedOutsideTransaction() {
        context.publishEvent(new TestDomainEvent());

        assertThat(context.getBean(TestEventListener.class).received).isEmpty();
    }

    private void publishWithinTransaction(boolean rollback) {
        ApplicationEventPublisher publisher = context;
        TransactionTemplate transactionTemplate = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transactionTemplate.executeWithoutResult(status -> {
            publisher.publishEvent(new TestDomainEvent());
            if (rollback) {
                status.setRollbackOnly();
            }
        });
    }

    static class TestDomainEvent extends DomainEvent {
    }

    @Component
    static class TestEventListener {

        final List<TestDomainEvent> received = new CopyOnWriteArrayList<>();

        @TransactionalEventListener
        void on(TestDomainEvent event) {
            received.add(event);
        }
    }

    @Configuration
    static class Config {

        // Spring Boot registers this automatically via @EnableTransactionManagement
        // once a PlatformTransactionManager bean is present (e.g. JPA autoconfiguration).
        // This bare AnnotationConfigApplicationContext skips Boot autoconfiguration, so
        // it's declared explicitly - without it, @TransactionalEventListener methods are
        // never invoked, silently, with no error at startup.
        @Bean
        static TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }

        @Bean
        TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }
}
