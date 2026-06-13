package com.flowforge.auth_service.controller;

import com.flowforge.auth_service.model.dto.UserResponse;
import com.flowforge.auth_service.service.interfaces.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity <UserResponse> me () {
        UserResponse user = userService.getCurrentUser();
        return ResponseEntity.ok(user);
    }
}
