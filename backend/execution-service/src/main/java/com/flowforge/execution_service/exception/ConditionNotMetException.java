package com.flowforge.execution_service.exception;

public class ConditionNotMetException extends RuntimeException {
    public ConditionNotMetException(String message) {
        super(message);
    }
}
