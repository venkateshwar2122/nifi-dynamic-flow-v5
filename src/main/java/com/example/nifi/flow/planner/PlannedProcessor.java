package com.example.nifi.flow.planner;

import com.example.nifi.flow.template.ProcessorTemplate;

import java.util.Map;

public class PlannedProcessor {

    private final String nodeId;
    private final String name;
    private final int positionIndex;
    private final ProcessorTemplate template;
    private final Map<String, Object> properties;

    public PlannedProcessor(
            String nodeId,
            String name,
            int positionIndex,
            ProcessorTemplate template,
            Map<String, Object> properties
    ) {
        this.nodeId = nodeId;
        this.name = name;
        this.positionIndex = positionIndex;
        this.template = template;
        this.properties = properties;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getName() {
        return name;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public ProcessorTemplate getTemplate() {
        return template;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
