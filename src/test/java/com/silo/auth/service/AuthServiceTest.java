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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private MemberLookup memberLookup;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final String EMAIL = "member@example.com";
    private static final String RAW_PASSWORD = "password123";
    private static final String PASSWORD_HASH = "hashed-password";

    @Test
    @DisplayName(
            "registerCredential rejects a memberId that doesn't exist")
    void registerCredential_throwsResourceNotFound_whenMemberDoesNotExist() {
        when(memberLookup.exists(MEMBER_ID)).thenReturn(false);

        assertThatThrownBy(() -> authService.registerCredential(
                new RegisterCredentialRequest(MEMBER_ID, RAW_PASSWORD)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(credentialRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "registerCredential rejects a member that already has credentials")
    void registerCredential_throwsDuplicateResource_whenCredentialAlreadyExists() {
        when(memberLookup.exists(MEMBER_ID)).thenReturn(true);
        when(credentialRepository.existsByMemberId(MEMBER_ID)).thenReturn(
                true);

        assertThatThrownBy(() -> authService.registerCredential(
                new RegisterCredentialRequest(MEMBER_ID, RAW_PASSWORD)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(credentialRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "registerCredential hashes the password and saves a MEMBER-role credential")
    void registerCredential_savesHashedCredential_whenMemberExistsAndHasNoCredential() {
        when(memberLookup.exists(MEMBER_ID)).thenReturn(true);
        when(credentialRepository.existsByMemberId(MEMBER_ID)).thenReturn(
                false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(
                PASSWORD_HASH);

        authService.registerCredential(
                new RegisterCredentialRequest(MEMBER_ID, RAW_PASSWORD));

        ArgumentCaptor<Credential> captor =
                ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(captor.capture());

        Credential saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(saved.getRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("login rejects an email with no matching member")
    void login_throwsBadCredentials_whenEmailNotFound() {
        when(memberLookup.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login rejects a member with no registered credentials")
    void login_throwsBadCredentials_whenCredentialNotFound() {
        when(memberLookup.findByEmail(EMAIL)).thenReturn(
                Optional.of(new MemberSummary(MEMBER_ID, EMAIL)));
        when(credentialRepository.findByMemberId(MEMBER_ID)).thenReturn(
                Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login rejects a wrong password")
    void login_throwsBadCredentials_whenPasswordDoesNotMatch() {
        Credential credential = Credential.builder()
                .memberId(MEMBER_ID)
                .passwordHash(PASSWORD_HASH)
                .role(Role.MEMBER)
                .build();
        when(memberLookup.findByEmail(EMAIL)).thenReturn(
                Optional.of(new MemberSummary(MEMBER_ID, EMAIL)));
        when(credentialRepository.findByMemberId(MEMBER_ID)).thenReturn(
                Optional.of(credential));
        when(passwordEncoder.matches(RAW_PASSWORD,
                PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest(EMAIL, RAW_PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName(
            "login returns a token and role when credentials are correct")
    void login_returnsToken_whenCredentialsAreCorrect() {
        Credential credential = Credential.builder()
                .memberId(MEMBER_ID)
                .passwordHash(PASSWORD_HASH)
                .role(Role.OFFICER)
                .build();
        when(memberLookup.findByEmail(EMAIL)).thenReturn(
                Optional.of(new MemberSummary(MEMBER_ID, EMAIL)));
        when(credentialRepository.findByMemberId(MEMBER_ID)).thenReturn(
                Optional.of(credential));
        when(passwordEncoder.matches(RAW_PASSWORD,
                PASSWORD_HASH)).thenReturn(true);
        when(jwtService.generateToken(MEMBER_ID, "OFFICER")).thenReturn(
                "signed-jwt");

        LoginResponse response =
                authService.login(new LoginRequest(EMAIL, RAW_PASSWORD));

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.memberId()).isEqualTo(MEMBER_ID);
        assertThat(response.role()).isEqualTo("OFFICER");
    }
}
