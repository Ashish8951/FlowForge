package com.flowforge.workflow_service.service.interfaces;

import com.flowforge.workflow_service.model.dto.CreateWorkflowRequest;
import com.flowforge.workflow_service.model.dto.SuccessResponse;
import com.flowforge.workflow_service.model.dto.WorkflowStepResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface WorkflowService  {
    SuccessResponse createWorkflow (CreateWorkflowRequest req);
    List<WorkflowStepResponse> getAllWorkflows();
    WorkflowStepResponse getWordflowById();
    SuccessResponse updateWorkflow();
    SuccessResponse deleteWorkflow();
}
