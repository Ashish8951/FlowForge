package com.flowforge.workflow_service.repository;
import java.util.List;
import com.flowforge.workflow_service.model.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep,Long> {
    List<WorkflowStep> findByWorkflowIdOrderByStepOrder(Long workflowId);
}
