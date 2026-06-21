package com.flowforge.workflow_service.model.dto;

import com.flowforge.workflow_service.model.enums.TriggerType;
import com.flowforge.workflow_service.model.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowRequest {
    private String name;
    private String description;
    private TriggerType triggerType;
    private String triggerConfig;
    private WorkflowStatus status;

}
