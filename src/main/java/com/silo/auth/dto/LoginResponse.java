package com.silo.auth.dto;

import java.util.UUID;

public record LoginResponse(String accessToken,
                            String refreshToken,
                            UUID memberId,
                            String role) {
}
