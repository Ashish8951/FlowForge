package com.flowforge.execution_service.model.dto;

import com.flowforge.execution_service.model.enums.ExecutionStatus;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExecutionResponse {
    private Long id;
    private Long workflowId;
    private Long userId;
    private ExecutionStatus status;
    private String triggerType;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private List<ExecutionStepResponse> steps;
}