package com.flowforge.auth_service.service.interfaces;

import com.flowforge.auth_service.model.dto.*;


public interface AuthService {
    SuccessResponse register(RegisterRequest req);

    LoginResponse login(LoginRequest req);

    String refreshToken(String token);

}
