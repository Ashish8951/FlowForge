package com.flowforge.auth_service.service.interfaces;

import com.flowforge.auth_service.model.entity.User;

public interface TokenService {
    String generateAccessToken(User user);

    boolean verifyToken(String token);

    String generateRefreshToken(User user);

    String extractEmail(String token);

    String extractUsername(String token);

    Long extractUserId(String token);
}
