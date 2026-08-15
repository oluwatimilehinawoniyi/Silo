package com.silo.member;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberLookup {

    boolean exists(UUID memberId);

    boolean isActiveAndVerified(UUID memberId);

    Optional<MemberSummary> findByEmail(String email);

    List<MemberSummary> findAllActiveAndVerified();
}
