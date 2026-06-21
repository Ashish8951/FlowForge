package com.flowforge.workflow_service.service.interfaces;

public interface TokenService {
    boolean verifyToken(String token);
    String extractEmail(String token);
    Long extractUserId(String token);
}
