package com.example.nifi.datastream.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "stream_node_templates")
public class StreamNodeTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "label")
    private String label;

    @Column(name = "icon")
    private String icon;

    @Column(name = "accessible")
    private Boolean accessible;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tabs", columnDefinition = "jsonb")
    private List<Map<String, Object>> tabs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_fields", columnDefinition = "jsonb")
    private List<Map<String, Object>> dataFields;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();

        if (this.accessible == null) {
            this.accessible = true;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public Boolean getAccessible() {
        return accessible;
    }

    public List<Map<String, Object>> getTabs() {
        return tabs;
    }

    public List<Map<String, Object>> getDataFields() {
        return dataFields;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setAccessible(Boolean accessible) {
        this.accessible = accessible;
    }

    public void setTabs(List<Map<String, Object>> tabs) {
        this.tabs = tabs;
    }

    public void setDataFields(List<Map<String, Object>> dataFields) {
        this.dataFields = dataFields;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
