package com.silo.auth.controller;

import com.silo.auth.dto.LoginRequest;
import com.silo.auth.dto.LoginResponse;
import com.silo.auth.dto.RefreshRequest;
import com.silo.auth.dto.RegisterCredentialRequest;
import com.silo.auth.service.AuthService;
import com.silo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Credential registration and login")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register credentials for an existing member",
            description = "Sets a login password for a member that already has a profile")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterCredentialRequest request) {
        authService.registerCredential(request);
        return ResponseEntity.ok(
                ApiResponse.success("Credentials registered successfully",
                        null));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate and receive an access token + refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Exchange a refresh token for a new access token",
            description = "Rotates the refresh token; the one presented is revoked and a new one is issued")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
