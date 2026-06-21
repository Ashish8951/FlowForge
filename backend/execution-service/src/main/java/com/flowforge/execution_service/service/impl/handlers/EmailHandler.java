package com.flowforge.execution_service.service.impl.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.execution_service.model.dto.WorkflowStepResponse;
import com.flowforge.execution_service.service.interfaces.StepHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailHandler implements StepHandler {

    private final ObjectMapper objectMapper;

    // Optional — only present when spring.mail.host is configured
    private JavaMailSender mailSender;

    public EmailHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public String getActionType() {
        return "EMAIL";
    }

    @Override
    public String execute(WorkflowStepResponse step) {
        if (mailSender == null) {
            throw new RuntimeException(
                    "EMAIL action unavailable: SMTP not configured — set spring.mail.host/port/username/password");
        }

        try {
            JsonNode config = objectMapper.readTree(step.getActionConfig());

            String to      = config.get("to").asText();
            String subject = config.get("subject").asText();
            String body    = config.get("body").asText();

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent — to={}, subject={}", to, subject);
            return "Email sent to " + to + " with subject: " + subject;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("EMAIL step failed: {}", e.getMessage());
            throw new RuntimeException("EMAIL failed: " + e.getMessage());
        }
    }
}
