package com.flowforge.execution_service.model.entity;

import com.flowforge.execution_service.model.enums.ExecutionStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_executions")
@Data
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long workflowId;
    private Long userId;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    private String triggerType;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
}