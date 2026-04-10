package ru.itmo.dto.auth;

import lombok.Data;

import java.util.Set;

@Data
public class LoginResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresInMs;
    private Set<String> effectiveRoles;
    private Set<String> privileges;

    public LoginResponse(
            String accessToken,
            long expiresInMs,
            Set<String> effectiveRoles,
            Set<String> privileges
    ) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresInMs = expiresInMs;
        this.effectiveRoles = effectiveRoles;
        this.privileges = privileges;
    }
}
