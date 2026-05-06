package com.example.nifi.service;

import com.example.nifi.datastreamcrud.entity.DatastreamNifiResourceEntity;
import com.example.nifi.datastreamcrud.repository.DatastreamNifiResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NifiResourceTrackingService {

    private final DatastreamNifiResourceRepository repository;

    public NifiResourceTrackingService(DatastreamNifiResourceRepository repository) {
        this.repository = repository;
    }

    public void saveResource(
            UUID datastreamId,
            String processGroupId,
            String resourceType,
            String resourceName,
            String resourceId,
            String nifiType
    ) {
        if (datastreamId == null || resourceId == null || resourceId.isBlank()) {
            return;
        }

        DatastreamNifiResourceEntity entity = new DatastreamNifiResourceEntity();

        entity.setDatastreamId(datastreamId);
        entity.setProcessGroupId(processGroupId);
        entity.setResourceType(resourceType);
        entity.setResourceName(resourceName);
        entity.setResourceId(resourceId);
        entity.setNifiType(nifiType);

        repository.save(entity);
    }

    public List<DatastreamNifiResourceEntity> getResources(UUID datastreamId) {
        return repository.findByDatastreamId(datastreamId);
    }
}