package com.flowforge.auth_service.model.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
@Data
public class LoginRequest {
    @Email(message = "Invalid email format")
    private String email;        // optional individually

    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must be alphanumeric")
    private String username;     // optional individually

    @NotBlank
    @Size(min = 8)
    private String password;
}