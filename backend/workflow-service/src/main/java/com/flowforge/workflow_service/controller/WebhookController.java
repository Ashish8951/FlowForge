package com.flowforge.workflow_service.controller;

import com.flowforge.workflow_service.exception.ResourceNotFoundException;
import com.flowforge.workflow_service.kafka.WorkflowTriggerProducer;
import com.flowforge.workflow_service.model.dto.SuccessResponse;
import com.flowforge.workflow_service.model.entity.Workflow;
import com.flowforge.workflow_service.model.enums.TriggerType;
import com.flowforge.workflow_service.model.enums.WorkflowStatus;
import com.flowforge.workflow_service.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WorkflowRepository workflowRepository;
    private final WorkflowTriggerProducer triggerProducer;

    /**
     * Public endpoint — no auth required.
     * External systems (Stripe, GitHub, etc.) POST here to fire a workflow.
     */
    @PostMapping("/{workflowId}")
    public ResponseEntity<SuccessResponse> receiveWebhook(
            @PathVariable Long workflowId,
            @RequestBody(required = false) String payload) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));

        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            return ResponseEntity.badRequest().body(
                    SuccessResponse.builder()
                            .success(false)
                            .message("Workflow is not active")
                            .build()
            );
        }

        if (workflow.getTriggerType() != TriggerType.WEBHOOK) {
            return ResponseEntity.badRequest().body(
                    SuccessResponse.builder()
                            .success(false)
                            .message("Workflow trigger type is not WEBHOOK")
                            .build()
            );
        }

        triggerProducer.publishTrigger(workflowId, workflow.getUserId(), "WEBHOOK");
        log.info("Webhook received for workflow {} — queued for execution", workflowId);

        return ResponseEntity.ok(SuccessResponse.builder()
                .success(true)
                .message("Workflow queued for execution")
                .build());
    }
}
