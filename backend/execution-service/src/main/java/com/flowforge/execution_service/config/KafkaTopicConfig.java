package com.flowforge.execution_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic workflowTriggerTopic() {
        return TopicBuilder.name("workflow.trigger")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic executionCompletedTopic() {
        return TopicBuilder.name("execution.completed")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic executionFailedTopic() {
        return TopicBuilder.name("execution.failed")
                .partitions(1)
                .replicas(1)
                .build();
    }
}