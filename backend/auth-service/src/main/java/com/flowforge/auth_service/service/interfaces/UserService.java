package com.flowforge.auth_service.service.interfaces;

import com.flowforge.auth_service.model.dto.UserResponse;

public interface UserService {
    UserResponse getCurrentUser();
}
