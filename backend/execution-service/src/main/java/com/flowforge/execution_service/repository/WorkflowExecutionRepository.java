package com.flowforge.execution_service.repository;

import com.flowforge.execution_service.model.entity.WorkflowExecution;
import com.flowforge.execution_service.model.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {
    List<WorkflowExecution> findByWorkflowId(Long workflowId);
    List<WorkflowExecution> findByUserId(Long userId);
    List<WorkflowExecution> findByUserIdAndStatus(Long userId, ExecutionStatus status);
}