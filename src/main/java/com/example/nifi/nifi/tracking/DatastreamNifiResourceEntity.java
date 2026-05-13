package com.example.nifi.nifi.tracking;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "datastream_nifi_resources")
public class DatastreamNifiResourceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "datastream_id", nullable = false)
    private UUID datastreamId;

    @Column(name = "deployment_run_id")
    private UUID deploymentRunId;

    @Column(name = "process_group_id")
    private String processGroupId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_name")
    private String resourceName;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(name = "nifi_type", length = 500)
    private String nifiType;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "parent_resource_id")
    private String parentResourceId;

    @Column(name = "source_resource_id")
    private String sourceResourceId;

    @Column(name = "destination_resource_id")
    private String destinationResourceId;

    @Column(name = "relationship_name")
    private String relationshipName;

    @Column(name = "resource_status")
    private String resourceStatus;

    @Column(name = "run_status")
    private String runStatus;

    @Column(name = "validation_status")
    private String validationStatus;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "is_current")
    private Boolean current;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
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

    public UUID getId() {
        return id;
    }

    public UUID getDatastreamId() {
        return datastreamId;
    }

    public void setDatastreamId(UUID datastreamId) {
        this.datastreamId = datastreamId;
    }

    public UUID getDeploymentRunId() {
        return deploymentRunId;
    }

    public void setDeploymentRunId(UUID deploymentRunId) {
        this.deploymentRunId = deploymentRunId;
    }

    public String getProcessGroupId() {
        return processGroupId;
    }

    public void setProcessGroupId(String processGroupId) {
        this.processGroupId = processGroupId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getNifiType() {
        return nifiType;
    }

    public void setNifiType(String nifiType) {
        this.nifiType = nifiType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getParentResourceId() {
        return parentResourceId;
    }

    public void setParentResourceId(String parentResourceId) {
        this.parentResourceId = parentResourceId;
    }

    public String getSourceResourceId() {
        return sourceResourceId;
    }

    public void setSourceResourceId(String sourceResourceId) {
        this.sourceResourceId = sourceResourceId;
    }

    public String getDestinationResourceId() {
        return destinationResourceId;
    }

    public void setDestinationResourceId(String destinationResourceId) {
        this.destinationResourceId = destinationResourceId;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public String getResourceStatus() {
        return resourceStatus;
    }

    public void setResourceStatus(String resourceStatus) {
        this.resourceStatus = resourceStatus;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getCurrent() {
        return current;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(OffsetDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
