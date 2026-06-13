package com.flowforge.auth_service.model.dto;

import lombok.*;

@Data
@Builder
public class SuccessResponse {
    boolean status;
    String message;
}
