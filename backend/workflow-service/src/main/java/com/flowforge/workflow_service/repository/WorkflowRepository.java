package com.flowforge.workflow_service.repository;

import com.flowforge.workflow_service.model.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowRepository extends JpaRepository <Workflow, Long> {
    List<Workflow> findByUserId(Long userId);

}
