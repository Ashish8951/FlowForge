package com.flowforge.execution_service.service.impl.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.execution_service.model.dto.WorkflowStepResponse;
import com.flowforge.execution_service.service.interfaces.StepHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class SlackWebhookHandler implements StepHandler {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public String getActionType() {
        return "SLACK_WEBHOOK";
    }

    @Override
    public String execute(WorkflowStepResponse step) {
        try {
            JsonNode config = objectMapper.readTree(step.getActionConfig());

            String webhookUrl = config.get("webhookUrl").asText();
            String message    = config.get("message").asText();

            log.info("Sending Slack message to webhook: {}", webhookUrl);

            String payload = "{\"text\":\"" + message + "\"}";

            String response = webClientBuilder.baseUrl(webhookUrl)
                    .build()
                    .post()
                    .uri("")
                    .header("Content-Type", "application/json")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Slack webhook response: {}", response);
            return "Slack message sent: " + message;

        } catch (Exception e) {
            log.error("SLACK_WEBHOOK step failed: {}", e.getMessage());
            throw new RuntimeException("SLACK_WEBHOOK failed: " + e.getMessage());
        }
    }
}
