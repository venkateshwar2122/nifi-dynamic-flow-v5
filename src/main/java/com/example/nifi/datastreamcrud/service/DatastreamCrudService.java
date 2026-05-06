package com.example.nifi.datastreamcrud.service;

import com.example.nifi.datastreamcrud.dto.DatastreamCreateRequest;
import com.example.nifi.datastreamcrud.dto.DatastreamUpdateRequest;
import com.example.nifi.datastreamcrud.entity.DataStreamEntity;
import com.example.nifi.datastreamcrud.repository.DatastreamCrudRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DatastreamCrudService {

    private final DatastreamCrudRepository repository;

    public DatastreamCrudService(DatastreamCrudRepository repository) {
        this.repository = repository;
    }

    public List<DataStreamEntity> listDatastreams(int skip, int limit) {
        if (skip < 0) {
            skip = 0;
        }

        if (limit <= 0) {
            limit = 100;
        }

        return repository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                DataStreamEntity::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ).reversed()
                )
                .skip(skip)
                .limit(limit)
                .toList();
    }

    public DataStreamEntity createDatastream(DatastreamCreateRequest request) {
        if (request.getDatastreamName() == null || request.getDatastreamName().isBlank()) {
            throw new IllegalArgumentException("datastreamName is required");
        }

        DataStreamEntity entity = new DataStreamEntity();

        entity.setDatastreamName(request.getDatastreamName());
        entity.setProcessGroupId(request.getProcessGroupId());
        entity.setDescription(request.getDescription());
        entity.setDatastreamType(request.getDatastreamType());
        entity.setDatastreamStatus(request.getDatastreamStatus());
        entity.setDeploymentStatus(request.getDeploymentStatus());
        entity.setStreamNodes(request.getStreamNodes());
        entity.setStreamEdges(request.getStreamEdges());
        entity.setUpdatedBy(request.getUpdatedBy() != null ? request.getUpdatedBy() : "admin");

        return repository.save(entity);
    }

    public DataStreamEntity getDatastreamById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Datastream not found with id: " + id));
    }

    public DataStreamEntity updateDatastream(UUID id, DatastreamUpdateRequest request) {
        DataStreamEntity entity = getDatastreamById(id);

        if (request.getDatastreamName() != null) {
            entity.setDatastreamName(request.getDatastreamName());
        }

        if (request.getProcessGroupId() != null) {
            entity.setProcessGroupId(request.getProcessGroupId());
        }

        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }

        if (request.getDatastreamType() != null) {
            entity.setDatastreamType(request.getDatastreamType());
        }

        if (request.getDatastreamStatus() != null) {
            entity.setDatastreamStatus(request.getDatastreamStatus());
        }

        if (request.getDeploymentStatus() != null) {
            entity.setDeploymentStatus(request.getDeploymentStatus());
        }

        if (request.getDeploymentError() != null) {
            entity.setDeploymentError(request.getDeploymentError());
        }

        if (request.getStreamNodes() != null) {
            entity.setStreamNodes(request.getStreamNodes());
        }

        if (request.getStreamEdges() != null) {
            entity.setStreamEdges(request.getStreamEdges());
        }

        if (request.getUpdatedBy() != null) {
            entity.setUpdatedBy(request.getUpdatedBy());
        }

        return repository.save(entity);
    }

    public void deleteDatastream(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Datastream not found with id: " + id);
        }

        repository.deleteById(id);
    }

    public List<String> deleteBatch(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids cannot be empty");
        }

        List<DataStreamEntity> datastreams = repository.findAllById(ids);

        if (datastreams.isEmpty()) {
            throw new RuntimeException("No datastreams found for given ids");
        }

        List<String> datastreamNames = datastreams.stream()
                .map(DataStreamEntity::getDatastreamName)
                .toList();

        repository.deleteAll(datastreams);

        return datastreamNames;
    }
}