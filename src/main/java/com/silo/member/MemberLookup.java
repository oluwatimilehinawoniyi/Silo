package com.silo.member;

import java.util.UUID;

public interface MemberLookup {

    boolean exists(UUID memberId);

    boolean isActiveAndVerified(UUID memberId);
}
