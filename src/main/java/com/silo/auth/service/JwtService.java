package com.silo.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${silo.jwt.secret}") String secret,
            @Value("${silo.jwt.expiration-minutes:15}")
            long expirationMinutes) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "silo.jwt.secret must be at least 256 bits (32 bytes) for HS256, got "
                            + secretBytes.length * 8 + " bits");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(UUID memberId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(memberId.toString())
                .claim("role", role)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(
                        now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
