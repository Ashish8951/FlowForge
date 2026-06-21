package com.flowforge.execution_service.controller;

import com.flowforge.execution_service.model.dto.AnalyticsSummaryResponse;
import com.flowforge.execution_service.model.dto.DeadLetterResponse;
import com.flowforge.execution_service.model.dto.ExecutionResponse;
import com.flowforge.execution_service.model.dto.TriggerRequest;
import com.flowforge.execution_service.model.enums.ExecutionStatus;
import com.flowforge.execution_service.service.interfaces.ExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping("/trigger/{workflowId}")
    public ResponseEntity<ExecutionResponse> triggerExecution(
            @PathVariable Long workflowId,
            @Valid @RequestBody TriggerRequest request
    ) {
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getCredentials();

        ExecutionResponse response = executionService.triggerExecution(
                workflowId, userId, request.getTriggerType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{executionId}")
    public ResponseEntity<ExecutionResponse> getExecution(
            @PathVariable Long executionId
    ) {
        return ResponseEntity.ok(executionService.getExecution(executionId));
    }

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<List<ExecutionResponse>> getExecutionsByWorkflow(
            @PathVariable Long workflowId
    ) {
        return ResponseEntity.ok(executionService.getExecutionsByWorkflow(workflowId));
    }

    // All executions for current user
    @GetMapping
    public ResponseEntity<List<ExecutionResponse>> getMyExecutions() {
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getCredentials();
        return ResponseEntity.ok(executionService.getExecutionsByUser(userId));
    }

    // Filter by status — /api/v1/executions/filter?status=FAILED
    @GetMapping("/filter")
    public ResponseEntity<List<ExecutionResponse>> getMyExecutionsByStatus(
            @RequestParam ExecutionStatus status
    ) {
        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getCredentials();
        return ResponseEntity.ok(executionService.getExecutionsByUserAndStatus(userId, status));
    }

    // Retry a failed execution from the failed step
    @PostMapping("/{executionId}/retry")
    public ResponseEntity<ExecutionResponse> retryExecution(@PathVariable Long executionId) {
        return ResponseEntity.ok(executionService.retryExecution(executionId));
    }

    // Dashboard analytics summary
    @GetMapping("/analytics/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalyticsSummary() {
        return ResponseEntity.ok(executionService.getAnalyticsSummary());
    }

    // Dead-letter queue viewer
    @GetMapping("/dlq")
    public ResponseEntity<List<DeadLetterResponse>> getDeadLetterEntries() {
        return ResponseEntity.ok(executionService.getDeadLetterEntries());
    }
}