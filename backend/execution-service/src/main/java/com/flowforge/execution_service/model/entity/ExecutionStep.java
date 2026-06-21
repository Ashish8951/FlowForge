package com.flowforge.execution_service.model.entity;

import com.flowforge.execution_service.model.enums.ActionType;
import com.flowforge.execution_service.model.enums.StepStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "execution_steps")
@Data
public class ExecutionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "execution_id")
    private WorkflowExecution execution;

    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    private StepStatus status;

    @Column(columnDefinition = "TEXT")
    private String result;

    private String errorMessage;
    private LocalDateTime executedAt;
}