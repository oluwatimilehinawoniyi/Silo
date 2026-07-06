package com.silo.common.logging.util;

import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationIdUtil {

    private static final String CORRELATION_ID = "correlationId";

    private CorrelationIdUtil() {
    }

    public static String getCorrelationId() {

        String correlationId = MDC.get(CORRELATION_ID);

        return correlationId != null
                ? correlationId
                : UUID.randomUUID().toString();
    }
}