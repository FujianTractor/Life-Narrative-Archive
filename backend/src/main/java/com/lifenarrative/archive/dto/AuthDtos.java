package com.lifenarrative.archive.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record RegisterRequest(
            String displayName,
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record UserProfile(
            String id,
            String username,
            String displayName
    ) {
    }

    public record AuthResponse(
            String accessToken,
            UserProfile user
    ) {
    }

    public record MeResponse(
            UserProfile user
    ) {
    }
}
