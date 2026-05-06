package com.example.nifi.datastreamcrud.entity;

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

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}