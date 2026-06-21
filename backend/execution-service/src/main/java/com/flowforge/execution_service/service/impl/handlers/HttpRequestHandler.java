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
public class HttpRequestHandler implements StepHandler {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public String getActionType() {
        return "HTTP_REQUEST";
    }

    @Override
    public String execute(WorkflowStepResponse step) {
        try {
            JsonNode config = objectMapper.readTree(step.getActionConfig());

            String url    = config.get("url").asText();
            String method = config.has("method") ? config.get("method").asText() : "GET";
            String body   = config.has("body")   ? config.get("body").asText()   : null;

            log.info("Executing HTTP_REQUEST — method: {}, url: {}", method, url);

            WebClient client = webClientBuilder.baseUrl(url).build();

            String response;

            if ("POST".equalsIgnoreCase(method)) {
                response = client.post()
                        .uri("")
                        .header("Content-Type", "application/json")
                        .bodyValue(body != null ? body : "{}")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } else {
                response = client.get()
                        .uri("")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            }

            log.info("HTTP_REQUEST completed — response: {}", response);
            return response != null ? response : "No response body";

        } catch (Exception e) {
            log.error("HTTP_REQUEST step failed: {}", e.getMessage());
            throw new RuntimeException("HTTP_REQUEST failed: " + e.getMessage());
        }
    }
}
