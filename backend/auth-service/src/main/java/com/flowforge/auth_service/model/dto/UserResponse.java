package com.flowforge.auth_service.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    Long id;
    String username;
    String email;

}
