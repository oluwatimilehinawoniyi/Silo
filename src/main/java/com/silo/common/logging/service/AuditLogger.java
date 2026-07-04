package com.silo.common.logging.service;

import com.silo.common.logging.enums.AuditAction;
import com.silo.common.logging.enums.AuditUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogger {

    private static final Logger auditLogger =
            LoggerFactory.getLogger("AUDIT");

    public void log(AuditUser user, AuditAction action, String details) {
        auditLogger.info(
                "USER: {} | ACTION: {} | DETAILS: {}",
                user,
                action,
                details
        );
    }

}
