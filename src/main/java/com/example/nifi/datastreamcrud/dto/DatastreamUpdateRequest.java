package com.example.nifi.datastreamcrud.dto;

import java.util.List;
import java.util.Map;

public class DatastreamUpdateRequest {

    private String datastreamName;
    private String processGroupId;
    private String description;
    private String datastreamType;
    private String datastreamStatus;
    private String deploymentStatus;
    private String deploymentError;
    private List<Map<String, Object>> streamNodes;
    private List<Map<String, Object>> streamEdges;
    private String updatedBy;

    public String getDatastreamName() {
        return datastreamName;
    }

    public void setDatastreamName(String datastreamName) {
        this.datastreamName = datastreamName;
    }

    public String getProcessGroupId() {
        return processGroupId;
    }

    public void setProcessGroupId(String processGroupId) {
        this.processGroupId = processGroupId;
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
}