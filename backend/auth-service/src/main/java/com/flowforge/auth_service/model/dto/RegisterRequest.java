package com.flowforge.auth_service.model.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
@Data
public class RegisterRequest {
    @NotBlank
    @Email
    private String  email;
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must be alphanumeric")
   private String username;
    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
   private String password;
}
