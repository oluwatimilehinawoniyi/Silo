package com.silo.auth.event;

import com.silo.common.event.DomainEvent;

import java.util.UUID;

public class OfficerApplicationSubmittedEvent extends DomainEvent {

    private final UUID applicationId;
    private final UUID applicantMemberId;

    public OfficerApplicationSubmittedEvent(UUID applicationId, UUID applicantMemberId) {
        this.applicationId = applicationId;
        this.applicantMemberId = applicantMemberId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getApplicantMemberId() {
        return applicantMemberId;
    }
}
