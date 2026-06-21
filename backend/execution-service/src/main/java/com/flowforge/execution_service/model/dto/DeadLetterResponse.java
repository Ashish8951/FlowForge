package com.flowforge.execution_service.model.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class DeadLetterResponse {
    private Long id;
    private Long executionId;
    private Long workflowId;
    private Integer stepOrder;
    private String actionType;
    private String payload;
    private String failureReason;
    private Integer retryCount;
    private boolean replayed;
    private LocalDateTime createdAt;
}
