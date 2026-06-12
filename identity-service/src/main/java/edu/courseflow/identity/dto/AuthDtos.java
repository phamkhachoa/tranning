package edu.courseflow.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String displayName
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds
    ) {
    }

    public record ProfileResponse(
            String userId,
            String email,
            String displayName,
            List<String> roles
    ) {
    }

    public record AuthzCheckRequest(
            @NotBlank String userId,
            @NotBlank String permission,
            String scopeType,
            String scopeId
    ) {
    }

    public record AuthzCheckResponse(boolean allowed, String reason) {
    }
}
