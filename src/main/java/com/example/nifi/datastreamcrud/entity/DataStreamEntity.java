package com.example.nifi.datastreamcrud.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "datastreams")
public class DataStreamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "\"processGroupId\"")
    private String processGroupId;

    @Column(name = "\"datastreamName\"", nullable = false)
    private String datastreamName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "\"datastreamType\"")
    private String datastreamType;

    @Column(name = "\"datastreamStatus\"")
    private String datastreamStatus;

    @Column(name = "\"deploymentStatus\"")
    private String deploymentStatus;

    @Column(name = "\"deploymentError\"", columnDefinition = "TEXT")
    private String deploymentError;

    @Column(name = "\"lastDeployedAt\"")
    private OffsetDateTime lastDeployedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"streamNodes\"", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> streamNodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "\"streamEdges\"", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> streamEdges;

    @Column(name = "\"createdAt\"")
    private OffsetDateTime createdAt;

    @Column(name = "\"updateAt\"")
    private OffsetDateTime updatedAt;

    @Column(name = "\"updatedBy\"")
    private String updatedBy;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
        }

        if (this.datastreamStatus == null) {
            this.datastreamStatus = "ACTIVE";
        }

        if (this.deploymentStatus == null) {
            this.deploymentStatus = "DRAFT";
        }

        if (this.streamNodes == null) {
            this.streamNodes = List.of();
        }

        if (this.streamEdges == null) {
            this.streamEdges = List.of();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();

        if (this.streamNodes == null) {
            this.streamNodes = List.of();
        }

        if (this.streamEdges == null) {
            this.streamEdges = List.of();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProcessGroupId() {
        return processGroupId;
    }

    public void setProcessGroupId(String processGroupId) {
        this.processGroupId = processGroupId;
    }

    public String getDatastreamName() {
        return datastreamName;
    }

    public void setDatastreamName(String datastreamName) {
        this.datastreamName = datastreamName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDatastreamType() {
        return datastreamType;
    }

    public void setDatastreamType(String datastreamType) {
        this.datastreamType = datastreamType;
    }

    public String getDatastreamStatus() {
        return datastreamStatus;
    }

    public void setDatastreamStatus(String datastreamStatus) {
        this.datastreamStatus = datastreamStatus;
    }

    public String getDeploymentStatus() {
        return deploymentStatus;
    }

    public void setDeploymentStatus(String deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }

    public List<Map<String, Object>> getStreamNodes() {
        return streamNodes;
    }

    public void setStreamNodes(List<Map<String, Object>> streamNodes) {
        this.streamNodes = streamNodes;
    }

    public List<Map<String, Object>> getStreamEdges() {
        return streamEdges;
    }

    public void setStreamEdges(List<Map<String, Object>> streamEdges) {
        this.streamEdges = streamEdges;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getDeploymentError() {
        return deploymentError;
    }

    public void setDeploymentError(String deploymentError) {
        this.deploymentError = deploymentError;
    }

    public OffsetDateTime getLastDeployedAt() {
        return lastDeployedAt;
    }

    public void setLastDeployedAt(OffsetDateTime lastDeployedAt) {
        this.lastDeployedAt = lastDeployedAt;
    }
}