package com.example.nifi.nifi.tracking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NifiResourceTrackingService {

    private static final Logger log = LoggerFactory.getLogger(NifiResourceTrackingService.class);

    private final DatastreamNifiResourceRepository repository;

    public NifiResourceTrackingService(DatastreamNifiResourceRepository repository) {
        this.repository = repository;
    }

    public DatastreamNifiResourceEntity saveResource(
            UUID datastreamId,
            String processGroupId,
            String resourceType,
            String resourceName,
            String resourceId,
            String nifiType
    ) {
        return saveResource(
                datastreamId,
                null,
                processGroupId,
                null,
                resourceType,
                resourceName,
                resourceId,
                nifiType,
                null,
                null,
                null,
                null,
                "CREATED",
                null,
                null,
                null,
                false,
                null
        );
    }

    public DatastreamNifiResourceEntity saveResource(
            UUID datastreamId,
            UUID deploymentRunId,
            String processGroupId,
            String tableName,
            String resourceType,
            String resourceName,
            String resourceId,
            String nifiType,
            String parentResourceId,
            String sourceResourceId,
            String destinationResourceId,
            String relationshipName,
            String resourceStatus,
            String runStatus,
            String validationStatus,
            Boolean enabled,
            Boolean current,
            String errorMessage
    ) {
        if (datastreamId == null || resourceId == null || resourceId.isBlank()) {
            log.warn("Skipping NiFi resource tracking because datastreamId/resourceId is missing. datastreamId={} resourceId={}",
                    datastreamId,
                    resourceId
            );
            return null;
        }

        DatastreamNifiResourceEntity entity = new DatastreamNifiResourceEntity();

        entity.setDatastreamId(datastreamId);
        entity.setDeploymentRunId(deploymentRunId);
        entity.setProcessGroupId(processGroupId);
        entity.setTableName(tableName);
        entity.setResourceType(resourceType);
        entity.setResourceName(resourceName);
        entity.setResourceId(resourceId);
        entity.setNifiType(nifiType);
        entity.setParentResourceId(parentResourceId);
        entity.setSourceResourceId(sourceResourceId);
        entity.setDestinationResourceId(destinationResourceId);
        entity.setRelationshipName(relationshipName);
        entity.setResourceStatus(resourceStatus);
        entity.setRunStatus(runStatus);
        entity.setValidationStatus(validationStatus);
        entity.setEnabled(enabled);
        entity.setCurrent(Boolean.TRUE.equals(current));
        entity.setErrorMessage(errorMessage);
        entity.setLastCheckedAt(OffsetDateTime.now());

        DatastreamNifiResourceEntity saved = repository.save(entity);
        log.info(
                "Tracked NiFi resource. datastreamId={} resourceType={} resourceName={} resourceId={} status={} runStatus={} validationStatus={}",
                datastreamId,
                resourceType,
                resourceName,
                resourceId,
                resourceStatus,
                runStatus,
                validationStatus
        );
        return saved;
    }

    public void markStatus(
            UUID datastreamId,
            String resourceId,
            String resourceStatus,
            String runStatus,
            String validationStatus,
            Boolean enabled,
            String errorMessage
    ) {
        if (datastreamId == null || resourceId == null || resourceId.isBlank()) {
            return;
        }

        repository.findFirstByDatastreamIdAndResourceId(datastreamId, resourceId)
                .ifPresentOrElse(entity -> {
                    entity.setResourceStatus(resourceStatus);
                    entity.setRunStatus(runStatus);
                    entity.setValidationStatus(validationStatus);
                    entity.setEnabled(enabled);
                    entity.setErrorMessage(errorMessage);
                    entity.setLastCheckedAt(OffsetDateTime.now());
                    repository.save(entity);
                    log.info(
                            "Updated tracked NiFi resource status. datastreamId={} resourceId={} status={} runStatus={} validationStatus={} enabled={}",
                            datastreamId,
                            resourceId,
                            resourceStatus,
                            runStatus,
                            validationStatus,
                            enabled
                    );
                }, () -> log.warn(
                        "Could not update tracked NiFi resource because row was not found. datastreamId={} resourceId={}",
                        datastreamId,
                        resourceId
                ));
    }

    public void markFailed(UUID datastreamId, String resourceId, String errorMessage) {
        markStatus(datastreamId, resourceId, "FAILED", null, null, false, errorMessage);
    }

    @Transactional
    public void markRunCurrent(UUID datastreamId, UUID deploymentRunId) {
        if (datastreamId == null || deploymentRunId == null) {
            return;
        }

        int cleared = repository.clearCurrentResources(datastreamId);
        int marked = repository.markRunResourcesCurrent(datastreamId, deploymentRunId);
        log.info(
                "Marked deployment run resources as current. datastreamId={} deploymentRunId={} cleared={} marked={}",
                datastreamId,
                deploymentRunId,
                cleared,
                marked
        );
    }

    public List<DatastreamNifiResourceEntity> getResources(UUID datastreamId) {
        return repository.findByDatastreamIdOrderByCreatedAtAsc(datastreamId);
    }

    public void deleteResources(UUID datastreamId) {
        repository.deleteByDatastreamId(datastreamId);
    }
}
