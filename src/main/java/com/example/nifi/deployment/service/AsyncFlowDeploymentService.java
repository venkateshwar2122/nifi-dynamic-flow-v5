package com.example.nifi.deployment.service;

import com.example.nifi.datastream.entity.DataStreamEntity;
import com.example.nifi.datastream.repository.DatastreamRepository;
import com.example.nifi.flow.dto.FlowRequest;
import com.example.nifi.flow.mapper.FlowRequestMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AsyncFlowDeploymentService {

    private final DatastreamRepository repository;
    private final FlowRequestMapper mapper;
    private final FlowBuilderService flowBuilderService;

    public AsyncFlowDeploymentService(
            DatastreamRepository repository,
            FlowRequestMapper mapper,
            FlowBuilderService flowBuilderService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.flowBuilderService = flowBuilderService;
    }

    @Async
    public void deployAsync(UUID datastreamId) {

        DataStreamEntity entity = repository.findById(datastreamId)
                .orElseThrow(() -> new RuntimeException("Datastream not found: " + datastreamId));

        try {
            entity.setDeploymentStatus("CREATING");
            entity.setDeploymentError(null);
            entity.setUpdatedAt(OffsetDateTime.now());
            repository.save(entity);

            FlowRequest request = mapper.fromEntity(entity);

            String processGroupId = flowBuilderService.buildFlow(request);

            entity.setProcessGroupId(processGroupId);
            entity.setDeploymentStatus("RUNNING");
            entity.setDatastreamStatus("ACTIVE");
            entity.setDeploymentError(null);
            entity.setLastDeployedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());

            repository.save(entity);

        } catch (Exception e) {
            entity.setDeploymentStatus("FAILED");
            entity.setDatastreamStatus("FAILED");
            entity.setDeploymentError(e.getMessage());
            entity.setUpdatedAt(OffsetDateTime.now());

            repository.save(entity);
        }
    }
}
