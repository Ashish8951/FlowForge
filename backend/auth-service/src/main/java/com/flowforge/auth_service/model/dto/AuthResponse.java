package com.flowforge.auth_service.model.dto;
import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
}
