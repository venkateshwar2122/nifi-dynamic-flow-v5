package com.example.nifi.nifi.processor;

import com.example.nifi.flow.layout.FlowLayoutService;
import com.example.nifi.flow.planner.PlannedProcessor;
import com.example.nifi.nifi.client.NiFiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessorManager {

    private static final Logger log = LoggerFactory.getLogger(ProcessorManager.class);

    private final NiFiClient client;
    private final FlowLayoutService layoutService;

    public ProcessorManager(NiFiClient client, FlowLayoutService layoutService) {
        this.client = client;
        this.layoutService = layoutService;
    }

    public ProcessorInfo createProcessor(String token, String processGroupId, PlannedProcessor plannedProcessor) {

        String type = plannedProcessor.getTemplate().getProcessorType();
        log.info("Creating NiFi processor: {} ({})", plannedProcessor.getName(), type);

        String id = client.createProcessor(
                token,
                processGroupId,
                type,
                layoutService.horizontal(plannedProcessor.getPositionIndex())
        );

        int version = client.getVersion(token, id, "processors");
        List<String> relationships = client.getRelationships(token, id);

        client.updateProcessorFull(
                token,
                id,
                version,
                plannedProcessor.getProperties(),
                plannedProcessor.getTemplate().getSchedulingPeriod(),
                relationships
        );

        return new ProcessorInfo(
                id,
                type,
                plannedProcessor.getTemplate().getMainRelationship()
        );
    }

    public static class ProcessorInfo {
        private final String id;
        private final String type;
        private final String mainRelationship;

        public ProcessorInfo(String id, String type, String mainRelationship) {
            this.id = id;
            this.type = type;
            this.mainRelationship = mainRelationship;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getMainRelationship() {
            return mainRelationship;
        }
    }
}
