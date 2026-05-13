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
        log.info("Starting table pipeline deployment. datastreamId={} table={} parentProcessGroupId={}",
                datastreamId,
                tableName,
                parentProcessGroupId
        );

        String childProcessGroupId = client.createPG(token, parentProcessGroupId, tableName);

        trackingService.saveResource(
                datastreamId,
                childProcessGroupId,
                tableName,
                "TABLE_PROCESS_GROUP",
                tableName,
                childProcessGroupId,
                "PROCESS_GROUP",
                parentProcessGroupId,
                null,
                null,
                null,
                "CREATED",
                "STOPPED",
                null,
                false,
                null
        );

        PipelinePlan plan = pipelinePlanner.planTablePipeline(tableContext, services);
        log.info("Pipeline plan created. table={} processorCount={} connectionCount={}",
                tableName,
                plan.getProcessors().size(),
                plan.getConnections().size()
        );
        Map<String, ProcessorManager.ProcessorInfo> processorsByNodeId = new HashMap<>();

        for (PlannedProcessor plannedProcessor : plan.getProcessors()) {
            log.info("Creating planned processor. table={} nodeId={} name={} role={} type={}",
                    tableName,
                    plannedProcessor.getNodeId(),
                    plannedProcessor.getName(),
                    plannedProcessor.getTemplate().getRole(),
                    plannedProcessor.getTemplate().getProcessorType()
            );
            ProcessorManager.ProcessorInfo processor =
                    processorManager.createProcessor(token, childProcessGroupId, plannedProcessor);

            processorsByNodeId.put(plannedProcessor.getNodeId(), processor);

            trackingService.saveResource(
                    datastreamId,
                    childProcessGroupId,
                    tableName,
                    "PROCESSOR",
                    plannedProcessor.getName(),
                    processor.getId(),
                    processor.getType(),
                    childProcessGroupId,
                    null,
                    null,
                    null,
                    "CREATED",
                    "STOPPED",
                    "PENDING",
                    false,
                    null
            );
        }

        for (PlannedConnection plannedConnection : plan.getConnections()) {
            log.info("Creating planned connection. table={} sourceNodeId={} destinationNodeId={}",
                    tableName,
                    plannedConnection.getSourceNodeId(),
                    plannedConnection.getDestinationNodeId()
            );
            ProcessorManager.ProcessorInfo source =
                    processorsByNodeId.get(plannedConnection.getSourceNodeId());
            ProcessorManager.ProcessorInfo destination =
                    processorsByNodeId.get(plannedConnection.getDestinationNodeId());

            if (source == null || destination == null) {
                throw new RuntimeException(
                        "Planned connection references missing processor. table="
                                + tableName
                                + " sourceNodeId="
                                + plannedConnection.getSourceNodeId()
                                + " destinationNodeId="
                                + plannedConnection.getDestinationNodeId()
                );
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
                    tableName,
                    "CONNECTION",
                    tableName + "_" + plannedConnection.getSourceNodeId() + "_to_" + plannedConnection.getDestinationNodeId(),
                    connectionId,
                    "CONNECTION",
                    childProcessGroupId,
                    source.getId(),
                    destination.getId(),
                    mainRelationship,
                    "CREATED",
                    "ACTIVE",
                    null,
                    true,
                    null
            );
        }

        for (ProcessorManager.ProcessorInfo processor : processorsByNodeId.values()) {
            try {
                client.validateProcessor(token, processor.getId());
                trackingService.markStatus(
                        datastreamId,
                        processor.getId(),
                        "VALID",
                        "STOPPED",
                        "VALID",
                        false,
                        null
                );
            } catch (RuntimeException e) {
                trackingService.markFailed(datastreamId, processor.getId(), e.getMessage());
                throw e;
            }
        }

        client.controlProcessGroup(token, childProcessGroupId, "RUNNING");
        trackingService.markStatus(
                datastreamId,
                childProcessGroupId,
                "RUNNING",
                "RUNNING",
                null,
                true,
                null
        );

        log.info("Table pipeline started. table={} processGroupId={}", tableName, childProcessGroupId);
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
