package com.flowforge.execution_service.service.interfaces;

import com.flowforge.execution_service.model.dto.WorkflowStepResponse;

public interface StepHandler {
    String getActionType();
    String execute(WorkflowStepResponse step);
}