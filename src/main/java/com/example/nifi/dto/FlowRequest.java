package com.example.nifi.dto;

import java.util.List;
import java.util.Map;

public class FlowRequest {

    private String datastreamName;
    private List<Map<String, Object>> streamNodes;
    private List<Map<String, Object>> streamEdges;

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