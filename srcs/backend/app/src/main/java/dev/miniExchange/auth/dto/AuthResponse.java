package dev.miniExchange.auth.dto;

public record AuthResponse(String token, long expiresAt) {
}

