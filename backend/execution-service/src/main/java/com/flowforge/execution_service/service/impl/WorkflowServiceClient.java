package com.flowforge.execution_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.execution_service.config.RedisKeys;
import com.flowforge.execution_service.model.dto.WorkflowDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WorkflowServiceClient {

    private final WebClient webClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String serviceKey;

    public WorkflowServiceClient(WebClient.Builder builder,
                                 StringRedisTemplate redis,
                                 ObjectMapper objectMapper,
                                 @Value("${workflow.service.url:http://localhost:8082}") String workflowServiceUrl,
                                 @Value("${internal.service.key:}") String serviceKey) {
        this.webClient = builder.baseUrl(workflowServiceUrl).build();
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
    }

    public WorkflowDetailResponse getWorkflow(Long workflowId) {
        String cacheKey = RedisKeys.workflowCache(workflowId);

        try {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit for workflow {}", workflowId);
                return objectMapper.readValue(cached, WorkflowDetailResponse.class);
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for workflow {} — falling back to API: {}", workflowId, e.getMessage());
        }

        try {
            WorkflowDetailResponse response = webClient.get()
                    .uri("/api/v1/workflows/internal/" + workflowId)
                    .header("X-Service-Key", serviceKey)
                    .retrieve()
                    .bodyToMono(WorkflowDetailResponse.class)
                    .block();

            if (response != null) {
                try {
                    redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response),
                            RedisKeys.WORKFLOW_CACHE_TTL_MIN, TimeUnit.MINUTES);
                    log.debug("Cached workflow {} for {} minutes", workflowId, RedisKeys.WORKFLOW_CACHE_TTL_MIN);
                } catch (Exception e) {
                    log.warn("Redis cache write failed for workflow {}: {}", workflowId, e.getMessage());
                }
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to fetch workflow {} from workflow-service: {}", workflowId, e.getMessage());
            return null;
        }
    }

    public void evictCache(Long workflowId) {
        redis.delete(RedisKeys.workflowCache(workflowId));
        log.debug("Evicted cache for workflow {}", workflowId);
    }
}
