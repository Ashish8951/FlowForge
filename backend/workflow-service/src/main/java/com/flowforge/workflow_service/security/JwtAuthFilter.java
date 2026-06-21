package com.flowforge.workflow_service.security;

import com.flowforge.workflow_service.service.interfaces.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    JwtAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer "))  {
            filterChain.doFilter(request, response);
            return;
        }
        token = token.substring(7);
        if(!tokenService.verifyToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        String email = tokenService.extractEmail(token);
        Long userId = tokenService.extractUserId(token);
        // Store both in authentication details
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        email,           // principal → email
                        userId,          // credentials → userId (we use this field to store it)
                        new ArrayList<>()
                );
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
