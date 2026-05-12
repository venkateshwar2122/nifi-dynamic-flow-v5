package com.example.nifi.flow.template;

import java.util.Map;

public class ProcessorTemplate {

    private final String key;
    private final ProcessorRole role;
    private final String processorType;
    private final String mainRelationship;
    private final String schedulingPeriod;
    private final Map<String, String> serviceProperties;

    public ProcessorTemplate(
            String key,
            ProcessorRole role,
            String processorType,
            String mainRelationship,
            String schedulingPeriod,
            Map<String, String> serviceProperties
    ) {
        this.key = key;
        this.role = role;
        this.processorType = processorType;
        this.mainRelationship = mainRelationship;
        this.schedulingPeriod = schedulingPeriod;
        this.serviceProperties = serviceProperties;
    }

    public String getKey() {
        return key;
    }

    public ProcessorRole getRole() {
        return role;
    }

    public String getProcessorType() {
        return processorType;
    }

    public String getMainRelationship() {
        return mainRelationship;
    }

    public String getSchedulingPeriod() {
        return schedulingPeriod;
    }

    public Map<String, String> getServiceProperties() {
        return serviceProperties;
    }
}
