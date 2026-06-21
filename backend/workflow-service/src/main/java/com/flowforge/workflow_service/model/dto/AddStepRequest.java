package com.flowforge.workflow_service.model.dto;

import com.flowforge.workflow_service.model.enums.ActionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class AddStepRequest {
    @NotNull
    private Integer stepOrder;
    @NotNull
    private ActionType actionType;
    private String actionConfig;
    private Integer maxRetries = 3;
}
