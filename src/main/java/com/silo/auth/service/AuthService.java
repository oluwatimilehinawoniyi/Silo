package com.silo.auth.service;

import com.silo.auth.dto.LoginRequest;
import com.silo.auth.dto.LoginResponse;
import com.silo.auth.dto.RegisterCredentialRequest;
import com.silo.auth.entity.Credential;
import com.silo.auth.entity.Role;
import com.silo.auth.repository.CredentialRepository;
import com.silo.common.exception.DuplicateResourceException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.member.MemberLookup;
import com.silo.member.MemberSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredentialRepository credentialRepository;
    private final MemberLookup memberLookup;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void registerCredential(RegisterCredentialRequest request) {
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

        credentialRepository.save(credential);
    }

    public LoginResponse login(LoginRequest request) {
        MemberSummary member = memberLookup.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid email or password"));

        Credential credential =
                credentialRepository.findByMemberId(member.id())
                        .orElseThrow(() -> new BadCredentialsException(
                                "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(),
                credential.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(member.id(),
                credential.getRole().name());
        return new LoginResponse(token, member.id(),
                credential.getRole().name());
    }
}
