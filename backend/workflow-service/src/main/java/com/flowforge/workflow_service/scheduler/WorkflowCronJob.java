package com.flowforge.workflow_service.scheduler;

import com.flowforge.workflow_service.kafka.WorkflowTriggerProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowCronJob {

    private final WorkflowTriggerProducer triggerProducer;

    @Job(name = "Workflow CRON trigger — wf-%0")
    public void fire(Long workflowId, Long userId) {
        log.info("CRON job firing for workflow {} (userId: {})", workflowId, userId);
        triggerProducer.publishTrigger(workflowId, userId, "SCHEDULE");
    }
}
