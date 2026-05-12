package com.example.nifi.deployment.service;

import com.example.nifi.flow.model.FlowContext;
import com.example.nifi.flow.planner.DynamicPipelinePlanner;
import com.example.nifi.flow.planner.PipelinePlan;
import com.example.nifi.flow.planner.PlannedConnection;
import com.example.nifi.flow.planner.PlannedProcessor;
import com.example.nifi.nifi.client.NiFiClient;
import com.example.nifi.nifi.processor.ProcessorManager;
import com.example.nifi.nifi.tracking.NifiResourceTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TablePipelineDeploymentService {

    private static final Logger log = LoggerFactory.getLogger(TablePipelineDeploymentService.class);

    private final NiFiClient client;
    private final ProcessorManager processorManager;
    private final NifiResourceTrackingService trackingService;
    private final DynamicPipelinePlanner pipelinePlanner;

    public TablePipelineDeploymentService(
            NiFiClient client,
            ProcessorManager processorManager,
            NifiResourceTrackingService trackingService,
            DynamicPipelinePlanner pipelinePlanner
    ) {
        this.client = client;
        this.processorManager = processorManager;
        this.trackingService = trackingService;
        this.pipelinePlanner = pipelinePlanner;
    }

    public String deployTablePipeline(
            String token,
            UUID datastreamId,
            String parentProcessGroupId,
            FlowContext tableContext,
            SharedControllerServices services
    ) {
        String tableName = tableContext.getTableName();
        log.info("Creating child process group for table: {}", tableName);

        String childProcessGroupId = client.createPG(token, parentProcessGroupId, tableName);

        trackingService.saveResource(
                datastreamId,
                childProcessGroupId,
                "TABLE_PROCESS_GROUP",
                tableName,
                childProcessGroupId,
                "PROCESS_GROUP"
        );

        PipelinePlan plan = pipelinePlanner.planTablePipeline(tableContext, services);
        Map<String, ProcessorManager.ProcessorInfo> processorsByNodeId = new HashMap<>();

        for (PlannedProcessor plannedProcessor : plan.getProcessors()) {
            ProcessorManager.ProcessorInfo processor =
                    processorManager.createProcessor(token, childProcessGroupId, plannedProcessor);

            processorsByNodeId.put(plannedProcessor.getNodeId(), processor);

            trackingService.saveResource(
                    datastreamId,
                    childProcessGroupId,
                    "PROCESSOR",
                    plannedProcessor.getName(),
                    processor.getId(),
                    processor.getType()
            );
        }

        for (PlannedConnection plannedConnection : plan.getConnections()) {
            ProcessorManager.ProcessorInfo source =
                    processorsByNodeId.get(plannedConnection.getSourceNodeId());
            ProcessorManager.ProcessorInfo destination =
                    processorsByNodeId.get(plannedConnection.getDestinationNodeId());

            if (source == null || destination == null) {
                throw new RuntimeException("Planned connection references missing processor");
            }

            String mainRelationship = source.getMainRelationship();
            List<String> allRelationships = client.getRelationships(token, source.getId());
            List<String> autoTerminate = allRelationships.stream()
                    .filter(relationship -> !relationship.equals(mainRelationship))
                    .toList();

            int firstVersion = client.getVersion(token, source.getId(), "processors");
            client.updateRelationships(token, source.getId(), firstVersion, autoTerminate);

            waitForNiFiConsistency();

            int secondVersion = client.getVersion(token, source.getId(), "processors");
            client.updateRelationships(token, source.getId(), secondVersion, autoTerminate);

            String connectionId = client.connect(
                    token,
                    childProcessGroupId,
                    source.getId(),
                    destination.getId(),
                    mainRelationship
            );

            trackingService.saveResource(
                    datastreamId,
                    childProcessGroupId,
                    "CONNECTION",
                    tableName + "_" + plannedConnection.getSourceNodeId() + "_to_" + plannedConnection.getDestinationNodeId(),
                    connectionId,
                    "CONNECTION"
            );
        }

        client.controlProcessGroup(token, childProcessGroupId, "RUNNING");

        log.info("Table pipeline started: {} ({})", tableName, childProcessGroupId);
        return childProcessGroupId;
    }

    private void waitForNiFiConsistency() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for NiFi consistency", e);
        }
    }

    public record SharedControllerServices(
            String dbcpId,
            String writerId,
            String readerId,
            String mongoId
    ) {
    }
}
