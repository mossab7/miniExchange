package dev.miniExchange.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
    @NotBlank
    String username,
    @NotBlank
    @Email
    String email,
    @NotBlank
    String password
) {
}