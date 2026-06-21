package com.flowforge.workflow_service.model.dto;
import com.flowforge.workflow_service.model.enums.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequest {

    @NotBlank
    private String name ;
    private String description ;
    @NotNull
    private TriggerType  triggerType;
    private String triggerConfig;
    List<AddStepRequest> steps;
}
