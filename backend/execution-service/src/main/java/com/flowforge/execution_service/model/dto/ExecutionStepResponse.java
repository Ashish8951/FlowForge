package com.flowforge.execution_service.model.dto;

import com.flowforge.execution_service.model.enums.ActionType;
import com.flowforge.execution_service.model.enums.StepStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExecutionStepResponse {
    private Long id;
    private Integer stepOrder;
    private ActionType actionType;
    private StepStatus status;
    private String result;
    private String errorMessage;
    private LocalDateTime executedAt;
}