package com.flowforge.execution_service.service.impl.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.execution_service.model.dto.WorkflowStepResponse;
import com.flowforge.execution_service.service.interfaces.StepHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DelayHandler implements StepHandler {

    private final ObjectMapper objectMapper;

    @Override
    public String getActionType() {
        return "DELAY";
    }

    @Override
    public String execute(WorkflowStepResponse step) {
        try {
            JsonNode config = objectMapper.readTree(step.getActionConfig());
            int seconds = config.has("seconds") ? config.get("seconds").asInt() : 5;

            log.info("Executing DELAY step — waiting {} seconds", seconds);
            Thread.sleep(seconds * 1000L);
            log.info("DELAY step completed after {} seconds", seconds);

            return "Delayed " + seconds + " seconds";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("DELAY interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("DELAY step failed: {}", e.getMessage());
            throw new RuntimeException("DELAY failed: " + e.getMessage());
        }
    }
}
