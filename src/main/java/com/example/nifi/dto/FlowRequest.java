package com.example.nifi.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlowRequest {

    private UUID datastreamId;
    private String datastreamName;
    private List<Map<String, Object>> streamNodes;
    private List<Map<String, Object>> streamEdges;

    public UUID getDatastreamId() {
        return datastreamId;
    }

    public void setDatastreamId(UUID datastreamId) {
        this.datastreamId = datastreamId;
    }

    public String getDatastreamName() {
        return datastreamName;
    }

    public void setDatastreamName(String datastreamName) {
        this.datastreamName = datastreamName;
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
}