package com.flowforge.execution_service.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SuccessResponse {
    String message;
    boolean success;
}
