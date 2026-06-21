package com.flowforge.execution_service.model.dto;

import lombok.Data;

@Data
public class WorkflowStepResponse {
    private Long id;
    private Integer stepOrder;
    private String actionType;
    private String actionConfig;
    private Integer maxRetries;
}