package com.example.nifi.flow.planner;

import java.util.List;

public class PipelinePlan {

    private final List<PlannedProcessor> processors;
    private final List<PlannedConnection> connections;

    public PipelinePlan(List<PlannedProcessor> processors, List<PlannedConnection> connections) {
        this.processors = processors;
        this.connections = connections;
    }

    public List<PlannedProcessor> getProcessors() {
        return processors;
    }

    public List<PlannedConnection> getConnections() {
        return connections;
    }
}
