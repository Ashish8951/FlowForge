package com.flowforge.execution_service.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowDetailResponse {
    private Long id;
    private String name;
    private String description;
    private Long userId;
    private String status;
    private String triggerType;
    private String triggerConfig;
    private List<WorkflowStepResponse> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}