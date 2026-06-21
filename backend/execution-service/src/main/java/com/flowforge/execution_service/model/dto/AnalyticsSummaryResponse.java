package com.flowforge.execution_service.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyticsSummaryResponse {
    private long totalExecutions;
    private long completedExecutions;
    private long failedExecutions;
    private long runningExecutions;
    private double successRate;
    private long dlqCount;
}
