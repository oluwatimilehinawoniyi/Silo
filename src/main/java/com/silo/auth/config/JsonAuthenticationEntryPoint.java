package com.silo.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.silo.common.exception.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Without this, Spring Security falls back to Http403ForbiddenEntryPoint for
 * any missing/invalid/expired JWT - a bare, bodyless 403 with no way to
 * tell "you're not logged in" apart from a real authorization failure. This
 * returns the same ErrorResponse shape every other error on the API uses,
 * with the 401 the docs already promise for this case.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED, "Authentication required - your session may have expired", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
