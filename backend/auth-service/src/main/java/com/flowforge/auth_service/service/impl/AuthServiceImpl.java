package com.flowforge.auth_service.service.impl;

import com.flowforge.auth_service.exception.InvalidCredentialsException;
import com.flowforge.auth_service.exception.ResourceAlreadyExistsException;
import com.flowforge.auth_service.exception.ResourceNotFoundException;
import com.flowforge.auth_service.model.dto.*;
import com.flowforge.auth_service.model.entity.User;
import com.flowforge.auth_service.service.interfaces.AuthService;
import com.flowforge.auth_service.repository.UserRepository;
import com.flowforge.auth_service.service.interfaces.TokenService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository,
                           TokenService tokenService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SuccessResponse register(RegisterRequest req) {

        if (req.getEmail() != null && userRepository.existsByEmail(req.getEmail())) {
            throw new ResourceAlreadyExistsException("emailId already used");
        }
        if (req.getUsername() != null && userRepository.existsByUsername(req.getUsername())) {
            throw new ResourceAlreadyExistsException("username already taken");
        }
        String hashedPassword = passwordEncoder.encode(req.getPassword());
        User user = User.builder()
                .email(req.getEmail())
                .username(req.getUsername())
                .password(hashedPassword)
                .build();
        userRepository.save(user);
        return SuccessResponse.builder().
                message("You have registered successfully").status(true).build();
    }

    @Override
    public LoginResponse login(LoginRequest req) {
        if (req.getEmail() == null && req.getUsername() == null) {
            throw new ResourceNotFoundException("please provide email or username ");
        }
        User user;
        if (req.getEmail() != null) {
            user = userRepository.findByEmail(req.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Email not found"));
        } else {
            user = userRepository.findByUsername(req.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("Username not found"));
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Password is incorrect");
        }
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        UserResponse userResponse = UserResponse.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail()).build();
        return LoginResponse.builder().accessToken(accessToken).refreshToken(refreshToken).user(userResponse).build();
    }

    @Override
    public String refreshToken(String token) {
        if (!tokenService.verifyToken(token)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }
        String email = tokenService.extractEmail(token);

        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new ResourceNotFoundException("Username not found"));
        String accessToken = tokenService.generateAccessToken(user);
        return accessToken;
    }


}
