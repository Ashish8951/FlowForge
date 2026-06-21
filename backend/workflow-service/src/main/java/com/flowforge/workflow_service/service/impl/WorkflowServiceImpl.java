package com.flowforge.workflow_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.workflow_service.exception.ResourceNotFoundException;
import com.flowforge.workflow_service.kafka.WorkflowTriggerProducer;
import com.flowforge.workflow_service.model.dto.*;
import com.flowforge.workflow_service.model.entity.Workflow;
import com.flowforge.workflow_service.model.entity.WorkflowStep;
import com.flowforge.workflow_service.model.enums.TriggerType;
import com.flowforge.workflow_service.model.enums.WorkflowStatus;
import com.flowforge.workflow_service.repository.WorkflowRepository;
import com.flowforge.workflow_service.repository.WorkflowStepRepository;
import com.flowforge.workflow_service.scheduler.WorkflowCronJob;
import com.flowforge.workflow_service.service.interfaces.WorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowTriggerProducer triggerProducer;
    private final JobScheduler jobScheduler;
    private final WorkflowCronJob workflowCronJob;
    private final ObjectMapper objectMapper;

    public WorkflowServiceImpl(WorkflowRepository workflowRepository,
                               WorkflowStepRepository workflowStepRepository,
                               WorkflowTriggerProducer triggerProducer,
                               JobScheduler jobScheduler,
                               WorkflowCronJob workflowCronJob,
                               ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.workflowStepRepository = workflowStepRepository;
        this.triggerProducer = triggerProducer;
        this.jobScheduler = jobScheduler;
        this.workflowCronJob = workflowCronJob;
        this.objectMapper = objectMapper;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getCredentials();
    }

    private WorkflowResponse mapToResponse(Workflow workflow) {
        List<WorkflowStepResponse> stepResponses = workflow.getSteps() == null
                ? List.of()
                : workflow.getSteps().stream()
                .map(step -> WorkflowStepResponse.builder()
                        .id(step.getId())
                        .stepOrder(step.getStepOrder())
                        .actionType(step.getActionType())
                        .actionConfig(step.getActionConfig())
                        .maxRetries(step.getMaxRetries())
                        .build())
                .collect(Collectors.toList());

        return WorkflowResponse.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .userId(workflow.getUserId())
                .status(workflow.getStatus())
                .triggerType(workflow.getTriggerType())
                .triggerConfig(workflow.getTriggerConfig())
                .steps(stepResponses)
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .build();
    }

    private String parseCronExpression(String triggerConfig) {
        if (triggerConfig == null || triggerConfig.isBlank()) {
            throw new IllegalArgumentException("SCHEDULE workflow requires triggerConfig with a 'cron' field, e.g. {\"cron\":\"0 * * * *\"}");
        }
        try {
            JsonNode node = objectMapper.readTree(triggerConfig);
            String cron = node.path("cron").asText(null);
            if (cron == null || cron.isBlank()) {
                throw new IllegalArgumentException("'cron' field is missing or empty in triggerConfig");
            }
            return cron;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse triggerConfig JSON: " + e.getMessage());
        }
    }

    @Override
    public WorkflowResponse createWorkflow(CreateWorkflowRequest req) {
        Long userId = getCurrentUserId();

        List<WorkflowStep> steps = req.getSteps() == null
                ? List.of()
                : req.getSteps().stream()
                .map(s -> WorkflowStep.builder()
                        .stepOrder(s.getStepOrder())
                        .actionType(s.getActionType())
                        .actionConfig(s.getActionConfig())
                        .maxRetries(s.getMaxRetries() != null ? s.getMaxRetries() : 3)
                        .build())
                .collect(Collectors.toList());

        Workflow workflow = Workflow.builder()
                .name(req.getName())
                .description(req.getDescription())
                .userId(userId)
                .triggerType(req.getTriggerType())
                .triggerConfig(req.getTriggerConfig())
                .build();

        steps.forEach(step -> step.setWorkflow(workflow));
        workflow.setSteps(steps);

        Workflow saved = workflowRepository.save(workflow);
        return mapToResponse(saved);
    }

    @Override
    public List<WorkflowResponse> getAllWorkflows() {
        Long userId = getCurrentUserId();
        return workflowRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WorkflowResponse getWorkflowById(Long id) {
        Long userId = getCurrentUserId();
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        if (!workflow.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Workflow not found");
        }
        return mapToResponse(workflow);
    }

    @Override
    public WorkflowResponse getWorkflowForService(Long id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));
        return mapToResponse(workflow);
    }

    @Override
    public SuccessResponse updateWorkflow(Long id, UpdateWorkflowRequest req) {
        Long userId = getCurrentUserId();
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        if (!workflow.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Workflow not found");
        }

        if (req.getName() != null) workflow.setName(req.getName());
        if (req.getDescription() != null) workflow.setDescription(req.getDescription());
        if (req.getTriggerType() != null) workflow.setTriggerType(req.getTriggerType());
        if (req.getTriggerConfig() != null) workflow.setTriggerConfig(req.getTriggerConfig());
        if (req.getStatus() != null) workflow.setStatus(req.getStatus());

        workflowRepository.save(workflow);
        return SuccessResponse.builder().success(true).message("Workflow updated successfully").build();
    }

    @Override
    public SuccessResponse deleteWorkflow(Long id) {
        Long userId = getCurrentUserId();
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        if (!workflow.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Workflow not found");
        }

        if (workflow.getTriggerType() == TriggerType.SCHEDULE) {
            try {
                jobScheduler.deleteRecurringJob("wf-" + id);
                log.info("Removed CRON job for deleted workflow {}", id);
            } catch (Exception e) {
                log.warn("No active CRON job to remove for workflow {}: {}", id, e.getMessage());
            }
        }

        workflowRepository.delete(workflow);
        return SuccessResponse.builder().success(true).message("Workflow deleted successfully").build();
    }

    @Override
    public WorkflowResponse activateWorkflow(Long id) {
        Long userId = getCurrentUserId();
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        if (!workflow.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Workflow not found");
        }

        workflow.setStatus(WorkflowStatus.ACTIVE);
        Workflow saved = workflowRepository.save(workflow);
        log.info("Workflow {} activated — trigger type: {}", id, workflow.getTriggerType());

        if (workflow.getTriggerType() == TriggerType.SCHEDULE) {
            String cronExpr = parseCronExpression(workflow.getTriggerConfig());
            String jobId = "wf-" + id;
            jobScheduler.scheduleRecurrently(jobId, cronExpr,
                    () -> workflowCronJob.fire(id, workflow.getUserId()));
            log.info("Registered recurring CRON job '{}' with expression: {}", jobId, cronExpr);
        }

        return mapToResponse(saved);
    }

    @Override
    public WorkflowResponse deactivateWorkflow(Long id) {
        Long userId = getCurrentUserId();
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        if (!workflow.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Workflow not found");
        }

        workflow.setStatus(WorkflowStatus.INACTIVE);
        Workflow saved = workflowRepository.save(workflow);

        if (workflow.getTriggerType() == TriggerType.SCHEDULE) {
            try {
                jobScheduler.deleteRecurringJob("wf-" + id);
                log.info("Removed CRON job for deactivated workflow {}", id);
            } catch (Exception e) {
                log.warn("No active CRON job to remove for workflow {}: {}", id, e.getMessage());
            }
        }

        log.info("Workflow {} deactivated", id);
        return mapToResponse(saved);
    }

    @Override
    public SuccessResponse triggerWorkflow(Long id) {
        Long userId = getCurrentUserId();
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        if (!workflow.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Workflow not found");
        }

        if (workflow.getStatus() != WorkflowStatus.ACTIVE) {
            throw new IllegalStateException("Workflow must be ACTIVE to trigger. Current status: " + workflow.getStatus());
        }
        if (workflow.getTriggerType() != TriggerType.MANUAL) {
            throw new IllegalStateException("Only MANUAL trigger type can be triggered via API. Use webhook for WEBHOOK triggers.");
        }

        triggerProducer.publishTrigger(workflow.getId(), userId, "MANUAL");
        log.info("Manual trigger published for workflow {}", id);

        return SuccessResponse.builder().success(true).message("Workflow queued for execution").build();
    }
}
