package com.flowforge.execution_service.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendExecutionCompleted(Long executionId, Long workflowId, String status) {
        String message = String.format(
                "{\"executionId\":%d,\"workflowId\":%d,\"status\":\"%s\"}",
                executionId, workflowId, status
        );
        kafkaTemplate.send("execution.completed", message);
        log.info("Sent execution.completed event: {}", message);
    }

    public void sendExecutionFailed(Long executionId, Long workflowId, String errorMessage) {
        String message = String.format(
                "{\"executionId\":%d,\"workflowId\":%d,\"errorMessage\":\"%s\"}",
                executionId, workflowId, errorMessage
        );
        kafkaTemplate.send("execution.failed", message);
        log.info("Sent execution.failed event: {}", message);
    }
}