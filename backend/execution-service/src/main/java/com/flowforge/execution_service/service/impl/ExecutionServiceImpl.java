package com.flowforge.execution_service.service.impl;

import com.flowforge.execution_service.exception.ConditionNotMetException;
import com.flowforge.execution_service.kafka.producer.ExecutionEventProducer;
import com.flowforge.execution_service.model.dto.AnalyticsSummaryResponse;
import com.flowforge.execution_service.model.dto.ExecutionResponse;
import com.flowforge.execution_service.model.dto.ExecutionStepResponse;
import com.flowforge.execution_service.model.dto.WorkflowDetailResponse;
import com.flowforge.execution_service.model.dto.WorkflowStepResponse;
import com.flowforge.execution_service.model.entity.DeadLetterEntry;
import com.flowforge.execution_service.model.entity.ExecutionStep;
import com.flowforge.execution_service.model.entity.WorkflowExecution;
import com.flowforge.execution_service.model.enums.ActionType;
import com.flowforge.execution_service.model.enums.ExecutionStatus;
import com.flowforge.execution_service.model.enums.StepStatus;
import com.flowforge.execution_service.repository.DeadLetterRepository;
import com.flowforge.execution_service.repository.ExecutionStepRepository;
import com.flowforge.execution_service.repository.WorkflowExecutionRepository;
import com.flowforge.execution_service.service.interfaces.ExecutionService;
import com.flowforge.execution_service.config.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionServiceImpl implements ExecutionService {

    private static final int BASE_DELAY_SECONDS = 5;

    private final WorkflowExecutionRepository executionRepository;
    private final ExecutionStepRepository stepRepository;
    private final ExecutionEventProducer eventProducer;
    private final WorkflowServiceClient workflowServiceClient;
    private final StepHandlerRegistry stepHandlerRegistry;
    private final DeadLetterRepository deadLetterRepository;
    private final StringRedisTemplate redis;
    private final AsyncExecutionRunner asyncRunner;
    private final RateLimiterService rateLimiter;

    // ── API-triggered execution ──────────────────────────────────────────────

    @Override
    public ExecutionResponse triggerExecution(Long workflowId, Long userId, String triggerType) {
        rateLimiter.checkRateLimit(userId);

        boolean isAlreadyActive = executionRepository.findByWorkflowId(workflowId)
                .stream()
                .anyMatch(e -> e.getStatus() == ExecutionStatus.RUNNING
                        || e.getStatus() == ExecutionStatus.QUEUED);
        if (isAlreadyActive) {
            throw new IllegalStateException(
                    "Workflow " + workflowId + " is already running or queued — wait for it to finish.");
        }

        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setUserId(userId);
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setTriggerType(triggerType);
        execution.setCreatedAt(LocalDateTime.now());
        execution.setStartedAt(LocalDateTime.now());
        execution = executionRepository.save(execution);

        log.info("Execution {} queued for workflow {}", execution.getId(), workflowId);

        final Long executionId = execution.getId();
        final Map<String, String> mdc = buildMdc(null, workflowId, executionId);
        asyncRunner.run(() -> {
            MDC.setContextMap(mdc);
            try {
                WorkflowExecution fresh = executionRepository.findById(executionId).orElseThrow();
                runExecution(fresh, workflowId, 0);
            } finally {
                MDC.clear();
            }
        });

        return mapToResponse(execution);
    }

    // ── Kafka-triggered execution ────────────────────────────────────────────

    @Override
    public void executeWorkflow(Long workflowId, Long userId, String triggerType, String correlationId) {
        // Gate 1 — deduplicate Kafka re-deliveries via correlationId + Redis SETNX
        if (correlationId != null) {
            String idempKey = RedisKeys.idempotencyKey(correlationId);
            try {
                Boolean isNew = redis.opsForValue().setIfAbsent(
                        idempKey, "1", RedisKeys.IDEMPOTENCY_TTL_HR, TimeUnit.HOURS);
                if (Boolean.FALSE.equals(isNew)) {
                    log.info("Duplicate correlationId={} — already processed, skipping", correlationId);
                    return;
                }
            } catch (Exception e) {
                log.warn("Redis idempotency check failed (proceeding): {}", e.getMessage());
            }
        }

        // Gate 2 — skip if workflow is already actively running
        boolean isAlreadyRunning = executionRepository.findByWorkflowId(workflowId)
                .stream().anyMatch(e -> e.getStatus() == ExecutionStatus.RUNNING);
        if (isAlreadyRunning) {
            log.info("Workflow {} already RUNNING — skipping concurrent trigger", workflowId);
            return;
        }

        // Find existing QUEUED (API path) or create new (Kafka/CRON path)
        WorkflowExecution execution = executionRepository.findByWorkflowId(workflowId)
                .stream()
                .filter(e -> e.getStatus() == ExecutionStatus.QUEUED
                        || e.getStatus() == ExecutionStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (execution == null) {
            execution = new WorkflowExecution();
            execution.setWorkflowId(workflowId);
            execution.setUserId(userId);
            execution.setStatus(ExecutionStatus.QUEUED);
            execution.setTriggerType(triggerType);
            execution.setCreatedAt(LocalDateTime.now());
            execution.setStartedAt(LocalDateTime.now());
            execution = executionRepository.save(execution);
            log.info("Created execution {} for workflow {} (trigger: {})", execution.getId(), workflowId, triggerType);
        }

        // Capture MDC and dispatch to thread pool — consumer thread returns immediately
        final Long executionId = execution.getId();
        final Map<String, String> mdc = buildMdc(correlationId, workflowId, executionId);
        asyncRunner.run(() -> {
            MDC.setContextMap(mdc);
            try {
                WorkflowExecution fresh = executionRepository.findById(executionId).orElseThrow();
                runExecution(fresh, workflowId, 0);
            } finally {
                MDC.clear();
            }
        });
    }

    // ── Retry from failed step ───────────────────────────────────────────────

    @Override
    public ExecutionResponse retryExecution(Long executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));

        if (execution.getStatus() != ExecutionStatus.FAILED) {
            throw new IllegalStateException("Only FAILED executions can be retried");
        }

        int failedFromStep = stepRepository.findByExecutionId(executionId)
                .stream()
                .filter(s -> s.getStatus() == StepStatus.FAILED)
                .mapToInt(ExecutionStep::getStepOrder)
                .min()
                .orElse(1);

        stepRepository.findByExecutionId(executionId).stream()
                .filter(s -> s.getStatus() == StepStatus.FAILED)
                .forEach(s -> {
                    s.setStatus(StepStatus.PENDING);
                    s.setErrorMessage(null);
                    stepRepository.save(s);
                });

        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setErrorMessage(null);
        executionRepository.save(execution);

        log.info("Retrying execution {} from step {}", executionId, failedFromStep);

        final Long workflowId = execution.getWorkflowId();
        final Map<String, String> mdc = buildMdc(null, workflowId, executionId);
        asyncRunner.run(() -> {
            MDC.setContextMap(mdc);
            try {
                WorkflowExecution fresh = executionRepository.findById(executionId).orElseThrow();
                runExecution(fresh, workflowId, failedFromStep);
            } finally {
                MDC.clear();
            }
        });

        return mapToResponse(executionRepository.findById(executionId).orElse(execution));
    }

    // ── Core runner ──────────────────────────────────────────────────────────

    private void runExecution(WorkflowExecution execution, Long workflowId, int resumeFromStep) {
        try {
            execution.setStatus(ExecutionStatus.RUNNING);
            executionRepository.save(execution);

            WorkflowDetailResponse workflow = workflowServiceClient.getWorkflow(workflowId);

            if (workflow == null || workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
                throw new RuntimeException("Workflow " + workflowId + " has no steps or could not be fetched");
            }

            boolean anyStepFailed = false;
            boolean conditionEarlyExit = false;

            for (WorkflowStepResponse stepDef : workflow.getSteps()) {
                if (stepDef.getStepOrder() < resumeFromStep) continue;

                boolean alreadyDone = stepRepository.findByExecutionId(execution.getId())
                        .stream()
                        .anyMatch(s -> s.getStepOrder().equals(stepDef.getStepOrder())
                                && s.getStatus() == StepStatus.COMPLETED);
                if (alreadyDone) continue;

                ExecutionStep step = new ExecutionStep();
                step.setExecution(execution);
                step.setStepOrder(stepDef.getStepOrder());
                step.setActionType(ActionType.valueOf(stepDef.getActionType()));
                step.setStatus(StepStatus.RUNNING);
                step.setExecutedAt(LocalDateTime.now());

                int maxRetries = stepDef.getMaxRetries() != null ? stepDef.getMaxRetries() : 3;

                try {
                    String result = executeWithRetry(stepDef, maxRetries);
                    step.setStatus(StepStatus.COMPLETED);
                    step.setResult(result);
                    log.info("Step {} completed for execution {}", stepDef.getStepOrder(), execution.getId());
                } catch (ConditionNotMetException e) {
                    // Condition evaluated false — graceful early exit, not a failure
                    step.setStatus(StepStatus.COMPLETED);
                    step.setResult("Condition not met — workflow exited early: " + e.getMessage());
                    conditionEarlyExit = true;
                    log.info("Step {} CONDITION not met for execution {} — exiting early",
                            stepDef.getStepOrder(), execution.getId());
                } catch (Exception e) {
                    step.setStatus(StepStatus.FAILED);
                    step.setErrorMessage(e.getMessage());
                    anyStepFailed = true;
                    log.error("Step {} failed after {} retries for execution {}: {}",
                            stepDef.getStepOrder(), maxRetries, execution.getId(), e.getMessage());
                    persistToDlq(execution, stepDef, e.getMessage(), maxRetries);
                }

                stepRepository.save(step);
                writeStateToRedis(execution.getId(), step.getStepOrder(), execution.getStatus().name());
                if (anyStepFailed || conditionEarlyExit) break;
            }

            if (anyStepFailed) {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setErrorMessage("One or more steps failed — see DLQ for details");
                execution.setCompletedAt(LocalDateTime.now());
                executionRepository.save(execution);
                writeStateToRedis(execution.getId(), null, "FAILED");
                eventProducer.sendExecutionFailed(execution.getId(), workflowId, execution.getErrorMessage());
                log.warn("Execution {} FAILED", execution.getId());
            } else {
                execution.setStatus(ExecutionStatus.COMPLETED);
                execution.setCompletedAt(LocalDateTime.now());
                executionRepository.save(execution);
                writeStateToRedis(execution.getId(), null, "COMPLETED");
                String completionReason = conditionEarlyExit ? "CONDITION_EXIT" : "COMPLETED";
                eventProducer.sendExecutionCompleted(execution.getId(), workflowId, completionReason);
                log.info("Execution {} COMPLETED (reason={})", execution.getId(), completionReason);
            }

        } catch (Exception e) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);
            writeStateToRedis(execution.getId(), null, "FAILED");
            eventProducer.sendExecutionFailed(execution.getId(), workflowId, e.getMessage());
            log.error("Execution {} FAILED with exception: {}", execution.getId(), e.getMessage());
        }
    }

    // ── Retry with exponential backoff ───────────────────────────────────────

    private String executeWithRetry(WorkflowStepResponse stepDef, int maxRetries) {
        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return stepHandlerRegistry.handle(stepDef);
            } catch (ConditionNotMetException e) {
                throw e; // conditions are not retried — propagate immediately
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxRetries) {
                    long delayMs = BASE_DELAY_SECONDS * (long) Math.pow(2, attempt) * 1000L;
                    log.warn("Step {} attempt {}/{} failed — retrying in {}ms: {}",
                            stepDef.getStepOrder(), attempt + 1, maxRetries, delayMs, e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted");
                    }
                }
            }
        }
        throw new RuntimeException("Step failed after " + maxRetries + " retries: "
                + (lastException != null ? lastException.getMessage() : "unknown error"));
    }

    // ── DLQ persistence ──────────────────────────────────────────────────────

    private void persistToDlq(WorkflowExecution execution, WorkflowStepResponse stepDef,
                               String failureReason, int retryCount) {
        DeadLetterEntry entry = new DeadLetterEntry();
        entry.setExecutionId(execution.getId());
        entry.setWorkflowId(execution.getWorkflowId());
        entry.setStepOrder(stepDef.getStepOrder());
        entry.setActionType(stepDef.getActionType());
        entry.setPayload(stepDef.getActionConfig());
        entry.setFailureReason(failureReason);
        entry.setRetryCount(retryCount);
        deadLetterRepository.save(entry);
        log.warn("DLQ entry created — execution={} step={}", execution.getId(), stepDef.getStepOrder());
    }

    // ── Analytics ───────────────────────────────────────────────────────────

    @Override
    public AnalyticsSummaryResponse getAnalyticsSummary() {
        List<WorkflowExecution> all = executionRepository.findAll();
        long total     = all.size();
        long completed = all.stream().filter(e -> e.getStatus() == ExecutionStatus.COMPLETED).count();
        long failed    = all.stream().filter(e -> e.getStatus() == ExecutionStatus.FAILED).count();
        long running   = all.stream().filter(e -> e.getStatus() == ExecutionStatus.RUNNING).count();
        double successRate = total > 0 ? (completed * 100.0 / total) : 0.0;
        long dlqCount = deadLetterRepository.countByReplayedFalse();

        return AnalyticsSummaryResponse.builder()
                .totalExecutions(total)
                .completedExecutions(completed)
                .failedExecutions(failed)
                .runningExecutions(running)
                .successRate(Math.round(successRate * 10.0) / 10.0)
                .dlqCount(dlqCount)
                .build();
    }

    @Override
    public List<com.flowforge.execution_service.model.dto.DeadLetterResponse> getDeadLetterEntries() {
        return deadLetterRepository.findByReplayedFalseOrderByCreatedAtDesc()
                .stream().map(this::mapDlqToResponse).collect(Collectors.toList());
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    @Override
    public ExecutionResponse getExecution(Long executionId) {
        return mapToResponse(executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId)));
    }

    @Override
    public List<ExecutionResponse> getExecutionsByWorkflow(Long workflowId) {
        return executionRepository.findByWorkflowId(workflowId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionResponse> getExecutionsByUser(Long userId) {
        return executionRepository.findByUserId(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionResponse> getExecutionsByUserAndStatus(Long userId, ExecutionStatus status) {
        return executionRepository.findByUserIdAndStatus(userId, status)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private ExecutionResponse mapToResponse(WorkflowExecution execution) {
        ExecutionResponse response = new ExecutionResponse();
        response.setId(execution.getId());
        response.setWorkflowId(execution.getWorkflowId());
        response.setUserId(execution.getUserId());
        response.setStatus(execution.getStatus());
        response.setTriggerType(execution.getTriggerType());
        response.setStartedAt(execution.getStartedAt());
        response.setCompletedAt(execution.getCompletedAt());
        response.setErrorMessage(execution.getErrorMessage());
        response.setCreatedAt(execution.getCreatedAt());
        List<ExecutionStep> steps = stepRepository.findByExecutionId(execution.getId());
        response.setSteps(steps.stream().map(this::mapStepToResponse).collect(Collectors.toList()));
        return response;
    }

    private ExecutionStepResponse mapStepToResponse(ExecutionStep step) {
        ExecutionStepResponse response = new ExecutionStepResponse();
        response.setId(step.getId());
        response.setStepOrder(step.getStepOrder());
        response.setActionType(step.getActionType());
        response.setStatus(step.getStatus());
        response.setResult(step.getResult());
        response.setErrorMessage(step.getErrorMessage());
        response.setExecutedAt(step.getExecutedAt());
        return response;
    }

    private com.flowforge.execution_service.model.dto.DeadLetterResponse mapDlqToResponse(DeadLetterEntry e) {
        return com.flowforge.execution_service.model.dto.DeadLetterResponse.builder()
                .id(e.getId())
                .executionId(e.getExecutionId())
                .workflowId(e.getWorkflowId())
                .stepOrder(e.getStepOrder())
                .actionType(e.getActionType())
                .payload(e.getPayload())
                .failureReason(e.getFailureReason())
                .retryCount(e.getRetryCount())
                .replayed(e.isReplayed())
                .createdAt(e.getCreatedAt())
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, String> buildMdc(String correlationId, Long workflowId, Long executionId) {
        Map<String, String> ctx = new java.util.HashMap<>();
        ctx.put("correlationId", correlationId != null ? correlationId : "-");
        ctx.put("workflowId",   String.valueOf(workflowId));
        ctx.put("executionId",  String.valueOf(executionId));
        return ctx;
    }

    private void writeStateToRedis(Long executionId, Integer currentStep, String status) {
        try {
            String value = String.format(
                    "{\"executionId\":%d,\"currentStep\":%s,\"status\":\"%s\"}",
                    executionId,
                    currentStep != null ? currentStep.toString() : "null",
                    status
            );
            redis.opsForValue().set(RedisKeys.executionState(executionId), value,
                    RedisKeys.EXECUTION_STATE_TTL_HR, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Redis state write failed for execution {}: {}", executionId, e.getMessage());
        }
    }
}
