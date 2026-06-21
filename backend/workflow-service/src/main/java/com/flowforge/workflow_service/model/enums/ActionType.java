package com.flowforge.workflow_service.model.enums;

public enum ActionType {
    HTTP_REQUEST,
    EMAIL,
    SLACK_WEBHOOK,
    DATABASE_INSERT,
    DELAY,          // wait X seconds before next step
    CONDITION       // if/else branching (future)
}
