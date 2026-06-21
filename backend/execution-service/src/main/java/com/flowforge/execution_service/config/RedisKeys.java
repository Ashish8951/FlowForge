package com.flowforge.execution_service.config;

public final class RedisKeys {

    private RedisKeys() {}

    public static final String WORKFLOW_CACHE_PREFIX  = "workflow:cache:";
    public static final long   WORKFLOW_CACHE_TTL_MIN = 5;

    public static final String EXECUTION_STATE_PREFIX = "execution:state:";
    public static final long   EXECUTION_STATE_TTL_HR = 1;

    public static final String IDEMPOTENCY_PREFIX  = "idempotency:";
    public static final long   IDEMPOTENCY_TTL_HR  = 24;

    public static final String RATE_LIMIT_PREFIX   = "rate:limit:";
    public static final long   RATE_LIMIT_TTL_SEC  = 60;

    public static String workflowCache(Long workflowId) {
        return WORKFLOW_CACHE_PREFIX + workflowId;
    }

    public static String executionState(Long executionId) {
        return EXECUTION_STATE_PREFIX + executionId;
    }

    public static String idempotencyKey(String correlationId) {
        return IDEMPOTENCY_PREFIX + correlationId;
    }

    public static String rateLimitKey(Long userId) {
        return RATE_LIMIT_PREFIX + userId;
    }
}
