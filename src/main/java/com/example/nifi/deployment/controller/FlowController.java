package com.example.nifi.deployment.controller;

import com.example.nifi.datastream.entity.DataStreamEntity;
import com.example.nifi.datastream.repository.DatastreamRepository;
import com.example.nifi.deployment.tracking.DeploymentRunEntity;
import com.example.nifi.deployment.tracking.DeploymentRunService;
import com.example.nifi.deployment.tracking.DeploymentTableRunService;
import com.example.nifi.flow.dto.FlowRequest;
import com.example.nifi.deployment.service.AsyncFlowDeploymentService;
import com.example.nifi.deployment.service.FlowBuilderService;
import com.example.nifi.nifi.tracking.NifiResourceTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/flow")
public class FlowController {

    private static final Logger log = LoggerFactory.getLogger(FlowController.class);

    private final FlowBuilderService service;
    private final DatastreamRepository repository;
    private final AsyncFlowDeploymentService asyncFlowDeploymentService;
    private final NifiResourceTrackingService trackingService;
    private final DeploymentRunService deploymentRunService;
    private final DeploymentTableRunService deploymentTableRunService;

    public FlowController(
            FlowBuilderService service,
            DatastreamRepository repository,
            AsyncFlowDeploymentService asyncFlowDeploymentService,
            NifiResourceTrackingService trackingService,
            DeploymentRunService deploymentRunService,
            DeploymentTableRunService deploymentTableRunService
    ) {
        this.service = service;
        this.repository = repository;
        this.asyncFlowDeploymentService = asyncFlowDeploymentService;
        this.trackingService = trackingService;
        this.deploymentRunService = deploymentRunService;
        this.deploymentTableRunService = deploymentTableRunService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createFlow(@RequestBody FlowRequest request) {
        log.info("Received direct flow create request. datastreamId={} datastreamName={}",
                request == null ? null : request.getDatastreamId(),
                request == null ? null : request.getDatastreamName()
        );

        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        if (request.getStreamNodes() == null || request.getStreamEdges() == null) {
            throw new IllegalArgumentException("Invalid JSON payload");
        }

        String processGroupId = service.buildFlow(request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Flow created successfully");
        response.put("processGroupId", processGroupId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/deploy/{datastreamId}")
    public ResponseEntity<?> deployDatastream(@PathVariable UUID datastreamId) {
        log.info("Received datastream deployment request. datastreamId={}", datastreamId);

        DataStreamEntity entity = repository.findById(datastreamId)
                .orElseThrow(() -> new RuntimeException("Datastream not found: " + datastreamId));

        entity.setDeploymentStatus("PENDING");
        entity.setDatastreamStatus("DEPLOYING");
        entity.setDeploymentError(null);
        entity.setUpdatedAt(OffsetDateTime.now());
        repository.save(entity);
        DeploymentRunEntity run = deploymentRunService.startRun(entity, "API");
        log.info("Datastream deployment queued. datastreamId={} runId={}", datastreamId, run.getId());

        asyncFlowDeploymentService.deployAsync(datastreamId, run.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Deployment started");
        response.put("datastreamId", datastreamId);
        response.put("deploymentRunId", run.getId());
        response.put("deploymentStatus", "PENDING");

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/status/{datastreamId}")
    public ResponseEntity<?> getDeploymentStatus(@PathVariable UUID datastreamId) {
        log.info("Fetching datastream deployment status. datastreamId={}", datastreamId);

        DataStreamEntity entity = repository.findById(datastreamId)
                .orElseThrow(() -> new RuntimeException("Datastream not found: " + datastreamId));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("datastreamId", entity.getId());
        response.put("datastreamName", entity.getDatastreamName());
        response.put("datastreamStatus", entity.getDatastreamStatus());
        response.put("deploymentStatus", entity.getDeploymentStatus());
        response.put("deploymentError", entity.getDeploymentError());
        response.put("processGroupId", entity.getProcessGroupId());
        response.put("lastDeployedAt", entity.getLastDeployedAt());
        response.put("updatedAt", entity.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/resources/{datastreamId}")
    public ResponseEntity<?> getNifiResources(@PathVariable UUID datastreamId) {
        log.info("Fetching tracked NiFi resources. datastreamId={}", datastreamId);
        return ResponseEntity.ok(trackingService.getResources(datastreamId));
    }

    @GetMapping("/runs/{datastreamId}")
    public ResponseEntity<?> getDeploymentRuns(@PathVariable UUID datastreamId) {
        log.info("Fetching deployment runs. datastreamId={}", datastreamId);
        return ResponseEntity.ok(deploymentRunService.getRuns(datastreamId));
    }

    @GetMapping("/runs/{deploymentRunId}/tables")
    public ResponseEntity<?> getDeploymentRunTables(@PathVariable UUID deploymentRunId) {
        log.info("Fetching deployment table runs. deploymentRunId={}", deploymentRunId);
        return ResponseEntity.ok(deploymentTableRunService.getTableRuns(deploymentRunId));
    }
}
