package com.flowforge.workflow_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowTriggerProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "workflow.trigger";

    public void publishTrigger(Long workflowId, Long userId, String triggerType) {
        String correlationId = UUID.randomUUID().toString();
        String message = String.format(
                "{\"workflowId\":%d,\"userId\":%d,\"triggerType\":\"%s\",\"correlationId\":\"%s\"}",
                workflowId, userId, triggerType, correlationId
        );
        kafkaTemplate.send(TOPIC, message);
        log.info("Published trigger — workflowId={}, userId={}, type={}, correlationId={}",
                workflowId, userId, triggerType, correlationId);
    }
}
