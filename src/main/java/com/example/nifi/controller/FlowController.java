package com.example.nifi.controller;

import com.example.nifi.datastreamcrud.entity.DataStreamEntity;
import com.example.nifi.datastreamcrud.repository.DatastreamCrudRepository;
import com.example.nifi.dto.FlowRequest;
import com.example.nifi.service.AsyncFlowDeploymentService;
import com.example.nifi.service.FlowBuilderService;
import com.example.nifi.service.NifiResourceTrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/flow")
public class FlowController {

    private final FlowBuilderService service;
    private final DatastreamCrudRepository repository;
    private final AsyncFlowDeploymentService asyncFlowDeploymentService;
    private final NifiResourceTrackingService trackingService;

    public FlowController(
            FlowBuilderService service,
            DatastreamCrudRepository repository,
            AsyncFlowDeploymentService asyncFlowDeploymentService,
            NifiResourceTrackingService trackingService
    ) {
        this.service = service;
        this.repository = repository;
        this.asyncFlowDeploymentService = asyncFlowDeploymentService;
        this.trackingService = trackingService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createFlow(@RequestBody FlowRequest request) {

        if (request == null) {
            return ResponseEntity.badRequest().body("Request body cannot be null");
        }

        if (request.getStreamNodes() == null || request.getStreamEdges() == null) {
            return ResponseEntity.badRequest().body("Invalid JSON payload");
        }

        String processGroupId = service.buildFlow(request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Flow created successfully");
        response.put("processGroupId", processGroupId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/deploy/{datastreamId}")
    public ResponseEntity<?> deployDatastream(@PathVariable UUID datastreamId) {

        DataStreamEntity entity = repository.findById(datastreamId)
                .orElseThrow(() -> new RuntimeException("Datastream not found: " + datastreamId));

        entity.setDeploymentStatus("PENDING");
        entity.setDeploymentError(null);
        entity.setUpdatedAt(OffsetDateTime.now());
        repository.save(entity);

        asyncFlowDeploymentService.deployAsync(datastreamId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Deployment started");
        response.put("datastreamId", datastreamId);
        response.put("deploymentStatus", "PENDING");

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/status/{datastreamId}")
    public ResponseEntity<?> getDeploymentStatus(@PathVariable UUID datastreamId) {

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
        return ResponseEntity.ok(trackingService.getResources(datastreamId));
    }
}