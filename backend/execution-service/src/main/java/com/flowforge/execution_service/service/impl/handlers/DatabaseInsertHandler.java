package com.flowforge.execution_service.service.impl.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.execution_service.model.dto.WorkflowStepResponse;
import com.flowforge.execution_service.service.interfaces.StepHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * Executes a parameterized SQL statement against an external JDBC datasource.
 *
 * actionConfig JSON shape:
 * {
 *   "datasourceUrl": "jdbc:postgresql://host:5432/mydb",
 *   "username": "dbuser",
 *   "password": "dbpass",
 *   "sql": "INSERT INTO audit_log(event, amount) VALUES (?, ?)",
 *   "params": ["payment_failed", "99.99"]
 * }
 * params is optional — omit it if the SQL has no placeholders.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInsertHandler implements StepHandler {

    private final ObjectMapper objectMapper;

    @Override
    public String getActionType() {
        return "DATABASE_INSERT";
    }

    @Override
    public String execute(WorkflowStepResponse step) {
        try {
            JsonNode config = objectMapper.readTree(step.getActionConfig());

            String url      = config.get("datasourceUrl").asText();
            String username = config.get("username").asText();
            String password = config.get("password").asText();
            String sql      = config.get("sql").asText();

            log.info("DATABASE_INSERT — connecting to {}", url);

            try (Connection conn = DriverManager.getConnection(url, username, password);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                if (config.has("params") && config.get("params").isArray()) {
                    JsonNode params = config.get("params");
                    for (int i = 0; i < params.size(); i++) {
                        ps.setString(i + 1, params.get(i).asText());
                    }
                }

                int affected = ps.executeUpdate();
                log.info("DATABASE_INSERT executed — {} row(s) affected", affected);
                return "SQL executed successfully — " + affected + " row(s) affected";
            }

        } catch (Exception e) {
            log.error("DATABASE_INSERT step failed: {}", e.getMessage());
            throw new RuntimeException("DATABASE_INSERT failed: " + e.getMessage());
        }
    }
}
