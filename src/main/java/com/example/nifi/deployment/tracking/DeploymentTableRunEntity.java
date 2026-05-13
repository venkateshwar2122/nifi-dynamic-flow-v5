package com.example.nifi.deployment.tracking;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "deployment_table_runs")
public class DeploymentTableRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "deployment_run_id", nullable = false)
    private UUID deploymentRunId;

    @Column(name = "datastream_id", nullable = false)
    private UUID datastreamId;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "process_group_id")
    private String processGroupId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_step")
    private String errorStep;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (startedAt == null) {
            startedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void complete(String status, String processGroupId, String errorStep, String errorMessage) {
        this.status = status;
        this.processGroupId = processGroupId;
        this.errorStep = errorStep;
        this.errorMessage = errorMessage;
        this.completedAt = OffsetDateTime.now();
        if (startedAt != null) {
            this.durationMs = Duration.between(startedAt, completedAt).toMillis();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getDeploymentRunId() {
        return deploymentRunId;
    }

    public void setDeploymentRunId(UUID deploymentRunId) {
        this.deploymentRunId = deploymentRunId;
    }

    public UUID getDatastreamId() {
        return datastreamId;
    }

    public void setDatastreamId(UUID datastreamId) {
        this.datastreamId = datastreamId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getProcessGroupId() {
        return processGroupId;
    }

    public void setProcessGroupId(String processGroupId) {
        this.processGroupId = processGroupId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getErrorStep() {
        return errorStep;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
