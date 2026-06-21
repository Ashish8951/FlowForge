package com.flowforge.workflow_service.model.dto;

import com.flowforge.workflow_service.model.enums.TriggerType;
import com.flowforge.workflow_service.model.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {
    private Long id;
    private String name;
    private String description;
    private Long  userId;
    private WorkflowStatus status;
    private TriggerType triggerType;
    private String triggerConfig;
    private List<WorkflowStepResponse> steps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
