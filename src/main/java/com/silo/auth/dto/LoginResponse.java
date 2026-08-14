package com.silo.auth.dto;

import java.util.UUID;

public record LoginResponse(String token, UUID memberId, String role) {
}
