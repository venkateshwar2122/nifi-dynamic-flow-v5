package com.example.nifi.deployment.service;

import com.example.nifi.deployment.tracking.DeploymentTableRunEntity;
import com.example.nifi.deployment.tracking.DeploymentTableRunService;
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
    private final DeploymentTableRunService tableRunService;

    public TablePipelineDeploymentService(
            NiFiClient client,
            ProcessorManager processorManager,
            NifiResourceTrackingService trackingService,
            DynamicPipelinePlanner pipelinePlanner,
            DeploymentTableRunService tableRunService
    ) {
        this.client = client;
        this.processorManager = processorManager;
        this.trackingService = trackingService;
        this.pipelinePlanner = pipelinePlanner;
        this.tableRunService = tableRunService;
    }

    public String deployTablePipeline(
            String token,
            UUID datastreamId,
            UUID deploymentRunId,
            String parentProcessGroupId,
            FlowContext tableContext,
            SharedControllerServices services
    ) {
        String tableName = tableContext.getTableName();
        DeploymentTableRunEntity tableRun = tableRunService.startTableRun(deploymentRunId, datastreamId, tableName);
        log.info("Starting table pipeline deployment. datastreamId={} table={} parentProcessGroupId={}",
                datastreamId,
                tableName,
                parentProcessGroupId
        );

        try {
            String childProcessGroupId = client.createPG(token, parentProcessGroupId, tableName);

            trackingService.saveResource(
                    datastreamId,
                    deploymentRunId,
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
                        deploymentRunId,
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
                        deploymentRunId,
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
                        false,
                        null
                );
            }

            for (ProcessorManager.ProcessorInfo processor : processorsByNodeId.values()) {
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

            tableRunService.markSuccess(tableRun.getId(), childProcessGroupId);
            log.info("Table pipeline started. table={} processGroupId={}", tableName, childProcessGroupId);
            return childProcessGroupId;
        } catch (RuntimeException e) {
            tableRunService.markFailed(tableRun.getId(), inferErrorStep(e), e.getMessage());
            throw e;
        }
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

    private String inferErrorStep(RuntimeException e) {
        String message = e.getMessage();
        if (message == null) {
            return "TABLE_PIPELINE";
        }

        if (message.contains("process group")) {
            return "TABLE_PROCESS_GROUP";
        }

        if (message.contains("processor")) {
            return "PROCESSOR";
        }

        if (message.contains("connection")) {
            return "CONNECTION";
        }

        return "TABLE_PIPELINE";
    }
}
