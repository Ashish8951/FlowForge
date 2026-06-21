package com.flowforge.execution_service.service.interfaces;

import com.flowforge.execution_service.model.dto.AnalyticsSummaryResponse;
import com.flowforge.execution_service.model.dto.DeadLetterResponse;
import com.flowforge.execution_service.model.dto.ExecutionResponse;
import com.flowforge.execution_service.model.enums.ExecutionStatus;

import java.util.List;

public interface ExecutionService {
    ExecutionResponse triggerExecution(Long workflowId, Long userId, String triggerType);
    void executeWorkflow(Long workflowId, Long userId, String triggerType, String correlationId);
    ExecutionResponse retryExecution(Long executionId);

    ExecutionResponse getExecution(Long executionId);
    List<ExecutionResponse> getExecutionsByWorkflow(Long workflowId);
    List<ExecutionResponse> getExecutionsByUser(Long userId);
    List<ExecutionResponse> getExecutionsByUserAndStatus(Long userId, ExecutionStatus status);

    AnalyticsSummaryResponse getAnalyticsSummary();
    List<DeadLetterResponse> getDeadLetterEntries();
}
