package com.silo.auth.service;

import com.silo.auth.dto.LoginRequest;
import com.silo.auth.dto.LoginResponse;
import com.silo.auth.dto.RefreshRequest;
import com.silo.auth.dto.RegisterCredentialRequest;
import com.silo.auth.entity.Credential;
import com.silo.auth.entity.Role;
import com.silo.auth.repository.CredentialRepository;
import com.silo.common.exception.DuplicateResourceException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final CredentialRepository credentialRepository;
    private final MemberLookup memberLookup;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshExpirationDays;

    public AuthService(
            CredentialRepository credentialRepository,
            MemberLookup memberLookup,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${silo.jwt.refresh-expiration-days}") long refreshExpirationDays) {
        this.credentialRepository = credentialRepository;
        this.memberLookup = memberLookup;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    public LoginResponse registerCredential(RegisterCredentialRequest request) {
        if (!memberLookup.exists(request.memberId())) {
            throw new ResourceNotFoundException(
                    "Member not found with id " + request.memberId());
        }

        if (credentialRepository.existsByMemberId(request.memberId())) {
            throw new DuplicateResourceException(
                    "Credentials already registered for this member");
        }

        Credential credential = Credential.builder()
                .memberId(request.memberId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.MEMBER)
                .build();

        credential = credentialRepository.save(credential);

        return issueTokens(credential);
    }

    public LoginResponse login(LoginRequest request) {
        MemberSummary member = memberLookup.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        Credential credential = credentialRepository.findByMemberId(member.id())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokens(credential);
    }

    public LoginResponse refresh(RefreshRequest request) {
        String hash = RefreshTokenGenerator.hash(request.refreshToken());

        Credential credential = credentialRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (credential.getRefreshTokenExpiresAt() == null
                || credential.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token expired or already used");
        }

        return issueTokens(credential);
    }

    private LoginResponse issueTokens(Credential credential) {
        String accessToken = jwtService.generateToken(credential.getMemberId(), credential.getRole().name());

        String rawRefreshToken = RefreshTokenGenerator.generate();
        credential.setRefreshTokenHash(RefreshTokenGenerator.hash(rawRefreshToken));
        credential.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays));
        credentialRepository.save(credential);

        return new LoginResponse(accessToken, rawRefreshToken, credential.getMemberId(), credential.getRole().name());
    }
}
