package com.flowforge.execution_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.execution_service.model.dto.WorkflowTriggerMessage;
import com.flowforge.execution_service.service.interfaces.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowTriggerConsumer {

    private final ExecutionService executionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "workflow.trigger", groupId = "execution-service-group")
    public void consumeWorkflowTrigger(String message) {
        try {
            WorkflowTriggerMessage trigger = objectMapper.readValue(message, WorkflowTriggerMessage.class);

            if (trigger.getCorrelationId() != null) MDC.put("correlationId", trigger.getCorrelationId());
            MDC.put("workflowId", String.valueOf(trigger.getWorkflowId()));

            log.info("Received workflow.trigger — workflowId={}, triggerType={}", trigger.getWorkflowId(), trigger.getTriggerType());

            executionService.executeWorkflow(
                    trigger.getWorkflowId(),
                    trigger.getUserId(),
                    trigger.getTriggerType(),
                    trigger.getCorrelationId()
            );
        } catch (Exception e) {
            log.error("Failed to process workflow.trigger event: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
}
