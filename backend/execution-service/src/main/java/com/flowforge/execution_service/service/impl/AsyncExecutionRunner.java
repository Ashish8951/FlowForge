package com.flowforge.execution_service.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AsyncExecutionRunner {

    @Async("executionTaskExecutor")
    public void run(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("Async execution task failed unexpectedly: {}", e.getMessage(), e);
        }
    }
}
