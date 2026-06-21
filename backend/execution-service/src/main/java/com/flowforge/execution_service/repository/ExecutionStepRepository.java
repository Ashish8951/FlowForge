package com.flowforge.execution_service.repository;

import com.flowforge.execution_service.model.entity.ExecutionStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionStepRepository extends JpaRepository<ExecutionStep, Long> {
    List<ExecutionStep> findByExecutionId(Long executionId);
}