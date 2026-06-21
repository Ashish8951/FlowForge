package com.flowforge.workflow_service.model.entity;

import com.flowforge.workflow_service.model.enums.ActionType;
import com.flowforge.workflow_service.model.enums.WorkflowStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_steps")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowStep {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false)
    private Integer stepOrder;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(nullable = false)
    private String actionConfig;

    @Column(nullable = false)
    private Integer maxRetries = 3;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
