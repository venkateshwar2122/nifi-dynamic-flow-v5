package com.example.nifi.flow.planner;

public class PlannedConnection {

    private final String sourceNodeId;
    private final String destinationNodeId;

    public PlannedConnection(String sourceNodeId, String destinationNodeId) {
        this.sourceNodeId = sourceNodeId;
        this.destinationNodeId = destinationNodeId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public String getDestinationNodeId() {
        return destinationNodeId;
    }
}
