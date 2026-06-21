package com.flowforge.workflow_service.controller;

import com.flowforge.workflow_service.model.dto.*;
import com.flowforge.workflow_service.service.interfaces.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    @Value("${internal.service.key:}")
    private String internalServiceKey;

    WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.createWorkflow(req));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflowById(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getWorkflowById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessResponse> updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkflowRequest req) {
        return ResponseEntity.ok(workflowService.updateWorkflow(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<SuccessResponse> deleteWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.deleteWorkflow(id));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<WorkflowResponse> activateWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.activateWorkflow(id));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<WorkflowResponse> deactivateWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.deactivateWorkflow(id));
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<SuccessResponse> triggerWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.triggerWorkflow(id));
    }

    /**
     * Internal endpoint for service-to-service calls (no user JWT required).
     * Callers must supply the shared X-Service-Key header.
     */
    @GetMapping("/internal/{id}")
    public ResponseEntity<?> getWorkflowInternal(
            @PathVariable Long id,
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey) {
        if (internalServiceKey.isBlank() || !internalServiceKey.equals(serviceKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or missing X-Service-Key");
        }
        return ResponseEntity.ok(workflowService.getWorkflowForService(id));
    }
}
