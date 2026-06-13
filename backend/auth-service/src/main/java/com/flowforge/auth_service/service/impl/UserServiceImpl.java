package com.flowforge.auth_service.service.impl;

import com.flowforge.auth_service.exception.ResourceNotFoundException;
import com.flowforge.auth_service.model.dto.UserResponse;
import com.flowforge.auth_service.model.entity.User;
import com.flowforge.auth_service.repository.UserRepository;
import com.flowforge.auth_service.service.interfaces.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) auth.getPrincipal();
        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new ResourceNotFoundException("Username not found"));

        return UserResponse.builder().email(user.getEmail()).id(user.getId()).username(user.getUsername()).build();
    }
}
