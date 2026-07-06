package com.silo.common.logging.controller;

import com.silo.common.logging.factory.AuditEventFactory;
import com.silo.common.logging.service.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/logging-test")
@RequiredArgsConstructor
public class LoggingTestController {

    private final AuditLogger auditLogger;
    private final AuditEventFactory auditEventFactory;

    @GetMapping("/audit")
    public String testAuditLogging() {

        auditLogger.log(
                auditEventFactory.contributionRecorded(
                        "SYSTEM",
                        "CONTRIB-001",
                        BigDecimal.valueOf(5000),
                        "GNF",
                        Map.of("source", "manual-test")
                )
        );

        return "Audit logging works!";
    }
}