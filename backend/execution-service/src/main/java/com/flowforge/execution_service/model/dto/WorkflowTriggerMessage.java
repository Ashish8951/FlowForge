package com.flowforge.execution_service.model.dto;

import lombok.Data;

@Data
public class WorkflowTriggerMessage {
    private Long workflowId;
    private Long userId;
    private String triggerType;
    private String correlationId;
}
