package com.example.nifi.deployment.service;

import com.example.nifi.datastream.entity.DataStreamEntity;
import com.example.nifi.datastream.repository.DatastreamRepository;
import com.example.nifi.deployment.tracking.DeploymentRunService;
import com.example.nifi.flow.dto.FlowRequest;
import com.example.nifi.flow.mapper.FlowRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AsyncFlowDeploymentService {

    private static final Logger log = LoggerFactory.getLogger(AsyncFlowDeploymentService.class);

    private final DatastreamRepository repository;
    private final FlowRequestMapper mapper;
    private final FlowBuilderService flowBuilderService;
    private final DeploymentRunService deploymentRunService;

    public AsyncFlowDeploymentService(
            DatastreamRepository repository,
            FlowRequestMapper mapper,
            FlowBuilderService flowBuilderService,
            DeploymentRunService deploymentRunService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.flowBuilderService = flowBuilderService;
        this.deploymentRunService = deploymentRunService;
    }

    @Async
    public void deployAsync(UUID datastreamId, UUID runId) {
        log.info("Starting async datastream deployment. datastreamId={} runId={}", datastreamId, runId);

        DataStreamEntity entity = repository.findById(datastreamId)
                .orElseThrow(() -> new RuntimeException("Datastream not found: " + datastreamId));

        try {
            deploymentRunService.markRunning(runId);

            entity.setDeploymentStatus("CREATING");
            entity.setDatastreamStatus("DEPLOYING");
            entity.setDeploymentError(null);
            entity.setUpdatedAt(OffsetDateTime.now());
            repository.save(entity);
            log.info("Datastream marked as CREATING. datastreamId={}", datastreamId);

            FlowRequest request = mapper.fromEntity(entity);
            log.info("Datastream mapped to flow request. datastreamId={} nodes={} edges={}",
                    datastreamId,
                    request.getStreamNodes() == null ? 0 : request.getStreamNodes().size(),
                    request.getStreamEdges() == null ? 0 : request.getStreamEdges().size()
            );

            String processGroupId = flowBuilderService.buildFlow(request, runId);
            log.info("Flow build completed. datastreamId={} processGroupId={}", datastreamId, processGroupId);
            deploymentRunService.markSuccess(runId, processGroupId);
            flowBuilderService.markDeploymentResourcesCurrent(datastreamId, runId);

            entity.setProcessGroupId(processGroupId);
            entity.setDeploymentStatus("SUCCESS");
            entity.setDatastreamStatus("DEPLOYED");
            entity.setDeploymentError(null);
            entity.setLastDeployedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());

            repository.save(entity);
            log.info("Datastream deployment completed successfully. datastreamId={} processGroupId={}",
                    datastreamId,
                    processGroupId
            );

        } catch (Exception e) {
            log.error("Datastream deployment failed. datastreamId={} error={}", datastreamId, e.getMessage(), e);
            deploymentRunService.markFailed(runId, inferErrorStep(e), e.getMessage());
            entity.setDeploymentStatus("FAILED");
            entity.setDatastreamStatus("FAILED");
            entity.setDeploymentError(e.getMessage());
            entity.setUpdatedAt(OffsetDateTime.now());

            repository.save(entity);
        }
    }

    private String inferErrorStep(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return "UNKNOWN";
        }

        if (message.contains("Flow parsing")) {
            return "FLOW_PARSING";
        }

        if (message.contains("Token fetch")) {
            return "NIFI_TOKEN";
        }

        if (message.contains("Controller Service")) {
            return "CONTROLLER_SERVICE";
        }

        if (message.contains("processor")) {
            return "PROCESSOR";
        }

        if (message.contains("connection")) {
            return "CONNECTION";
        }

        return "DEPLOYMENT";
    }
}
