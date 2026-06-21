package com.flowforge.execution_service.service.impl.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.execution_service.exception.ConditionNotMetException;
import com.flowforge.execution_service.model.dto.WorkflowStepResponse;
import com.flowforge.execution_service.service.interfaces.StepHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Evaluates a simple condition. If false, throws ConditionNotMetException which
 * causes the execution to exit early as COMPLETED (not FAILED — no DLQ entry).
 *
 * actionConfig JSON shape:
 * {
 *   "leftOperand":  "payment_status",
 *   "operator":     "equals",
 *   "rightOperand": "SUCCESS",
 *   "description":  "Only continue if payment succeeded"
 * }
 *
 * Supported operators: equals, not_equals, contains, not_contains, gt, lt
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConditionHandler implements StepHandler {

    private final ObjectMapper objectMapper;

    @Override
    public String getActionType() {
        return "CONDITION";
    }

    @Override
    public String execute(WorkflowStepResponse step) {
        try {
            JsonNode config = objectMapper.readTree(step.getActionConfig());

            String left        = config.get("leftOperand").asText();
            String operator    = config.get("operator").asText().toLowerCase();
            String right       = config.get("rightOperand").asText();
            String description = config.has("description") ? config.get("description").asText() : operator;

            boolean result = evaluate(left, operator, right);

            if (result) {
                log.info("CONDITION passed: {}", description);
                return "Condition passed: " + description;
            } else {
                log.info("CONDITION not met: {} — triggering early exit", description);
                throw new ConditionNotMetException(description);
            }

        } catch (ConditionNotMetException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("CONDITION step misconfigured: " + e.getMessage());
        }
    }

    private boolean evaluate(String left, String operator, String right) {
        return switch (operator) {
            case "equals"       -> left.equals(right);
            case "not_equals"   -> !left.equals(right);
            case "contains"     -> left.contains(right);
            case "not_contains" -> !left.contains(right);
            case "gt" -> {
                try { yield Double.parseDouble(left) > Double.parseDouble(right); }
                catch (NumberFormatException e) { yield left.compareTo(right) > 0; }
            }
            case "lt" -> {
                try { yield Double.parseDouble(left) < Double.parseDouble(right); }
                catch (NumberFormatException e) { yield left.compareTo(right) < 0; }
            }
            default -> throw new RuntimeException("Unknown operator: " + operator);
        };
    }
}
