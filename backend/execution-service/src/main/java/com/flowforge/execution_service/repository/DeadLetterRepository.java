package com.flowforge.execution_service.repository;

import com.flowforge.execution_service.model.entity.DeadLetterEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeadLetterRepository extends JpaRepository<DeadLetterEntry, Long> {
    List<DeadLetterEntry> findByExecutionId(Long executionId);
    List<DeadLetterEntry> findByReplayedFalseOrderByCreatedAtDesc();
    long countByReplayedFalse();
}
