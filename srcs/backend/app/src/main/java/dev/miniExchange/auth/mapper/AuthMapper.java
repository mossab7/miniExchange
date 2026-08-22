package dev.miniExchange.auth.mapper;

import dev.miniExchange.auth.dto.*;

public class AuthMapper {

    static public AuthResponse toResponse(String token, long expiresAt) {
        return new AuthResponse(token, expiresAt);
    }
}
