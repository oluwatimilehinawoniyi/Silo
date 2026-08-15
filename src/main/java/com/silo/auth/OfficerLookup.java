package com.silo.auth;

import java.util.List;
import java.util.UUID;

public interface OfficerLookup {

    boolean isOfficer(UUID memberId);

    List<UUID> findAllOfficerMemberIds();
}
