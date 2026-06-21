package com.flowforge.workflow_service.service.impl;

import com.flowforge.workflow_service.service.interfaces.TokenService;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;

@Service
public class JwtTokenServiceImpl implements TokenService {
    @Value("${jwt.secret}")
    private String secretKey;


    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public boolean verifyToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @Override
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }


    @Override
    public Long extractUserId(String token) {
        return getClaims(token).get("id", Long.class);
    }
}
