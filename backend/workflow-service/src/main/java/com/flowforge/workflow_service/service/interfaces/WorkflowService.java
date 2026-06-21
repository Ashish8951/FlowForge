package com.flowforge.workflow_service.service.interfaces;

import com.flowforge.workflow_service.model.dto.CreateWorkflowRequest;
import com.flowforge.workflow_service.model.dto.SuccessResponse;
import com.flowforge.workflow_service.model.dto.UpdateWorkflowRequest;
import com.flowforge.workflow_service.model.dto.WorkflowResponse;

import java.util.List;

public interface WorkflowService {
    WorkflowResponse createWorkflow(CreateWorkflowRequest req);
    List<WorkflowResponse> getAllWorkflows();
    WorkflowResponse getWorkflowById(Long id);
    SuccessResponse updateWorkflow(Long id, UpdateWorkflowRequest req);
    SuccessResponse deleteWorkflow(Long id);
    WorkflowResponse activateWorkflow(Long id);
    WorkflowResponse deactivateWorkflow(Long id);
    SuccessResponse triggerWorkflow(Long id);
    WorkflowResponse getWorkflowForService(Long id);
}
