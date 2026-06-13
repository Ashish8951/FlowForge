package com.flowforge.auth_service.controller;

import com.flowforge.auth_service.model.dto.*;
import com.flowforge.auth_service.service.interfaces.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    // this endpoint = POST /api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<SuccessResponse> register(@Valid @RequestBody RegisterRequest req) {
        SuccessResponse response = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // this endpoint = POST /api/v1/auth/login
    @PostMapping("/login")
    public  ResponseEntity <LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse response = authService.login(req);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/refreshToken")
    public ResponseEntity<SuccessResponse> refreshToken (@Valid @RequestBody RefreshTokenRequest req) {
        String accessToken = authService.refreshToken(req.getRefreshToken());
        return ResponseEntity.ok(SuccessResponse.builder().status(true).message(accessToken).build());
    }
}
