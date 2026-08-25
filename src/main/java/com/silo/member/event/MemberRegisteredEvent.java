package com.silo.member.event;

import com.silo.common.event.DomainEvent;

import java.util.UUID;

public class MemberRegisteredEvent extends DomainEvent {

    private final UUID memberId;

    public MemberRegisteredEvent(UUID memberId) {
        this.memberId = memberId;
    }

    public UUID getMemberId() {
        return memberId;
    }
}
