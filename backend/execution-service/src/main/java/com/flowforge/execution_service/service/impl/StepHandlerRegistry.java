package com.flowforge.execution_service.service.impl;

import com.flowforge.execution_service.model.dto.WorkflowStepResponse;
import com.flowforge.execution_service.service.interfaces.StepHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class StepHandlerRegistry {

    private final Map<String, StepHandler> handlers;

    public StepHandlerRegistry(List<StepHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(StepHandler::getActionType, Function.identity()));
        log.info("Registered step handlers: {}", handlers.keySet());
    }

    public String handle(WorkflowStepResponse step) {
        StepHandler handler = handlers.get(step.getActionType());
        if (handler == null) {
            log.warn("No handler registered for action type: {}", step.getActionType());
            return "Unhandled action type: " + step.getActionType();
        }
        return handler.execute(step);
    }
}
