package com.flowforge.execution_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TriggerRequest {
    @NotBlank(message = "Trigger type is required")
    private String triggerType;
}