package com.flowforge.workflow_service.model.dto;

import com.flowforge.workflow_service.model.enums.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowStepResponse {
    private Long id;
    private Integer stepOrder;
    private ActionType actionType;
    private String actionConfig;
    private Integer maxRetries;
}
