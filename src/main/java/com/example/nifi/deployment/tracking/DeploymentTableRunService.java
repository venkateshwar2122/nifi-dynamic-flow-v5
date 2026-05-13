package com.example.nifi.deployment.tracking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DeploymentTableRunService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentTableRunService.class);

    private final DeploymentTableRunRepository repository;

    public DeploymentTableRunService(DeploymentTableRunRepository repository) {
        this.repository = repository;
    }

    public DeploymentTableRunEntity startTableRun(UUID deploymentRunId, UUID datastreamId, String tableName) {
        DeploymentTableRunEntity tableRun = new DeploymentTableRunEntity();
        tableRun.setDeploymentRunId(deploymentRunId);
        tableRun.setDatastreamId(datastreamId);
        tableRun.setTableName(tableName);
        tableRun.setStatus("RUNNING");
        tableRun.setStartedAt(OffsetDateTime.now());

        DeploymentTableRunEntity saved = repository.save(tableRun);
        log.info(
                "Deployment table run started. tableRunId={} deploymentRunId={} datastreamId={} table={}",
                saved.getId(),
                deploymentRunId,
                datastreamId,
                tableName
        );
        return saved;
    }

    public void markSuccess(UUID tableRunId, String processGroupId) {
        repository.findById(tableRunId).ifPresent(tableRun -> {
            tableRun.complete("SUCCESS", processGroupId, null, null);
            repository.save(tableRun);
            log.info(
                    "Deployment table run succeeded. tableRunId={} table={} processGroupId={} durationMs={}",
                    tableRunId,
                    tableRun.getTableName(),
                    processGroupId,
                    tableRun.getDurationMs()
            );
        });
    }

    public void markFailed(UUID tableRunId, String errorStep, String errorMessage) {
        repository.findById(tableRunId).ifPresent(tableRun -> {
            tableRun.complete("FAILED", tableRun.getProcessGroupId(), errorStep, errorMessage);
            repository.save(tableRun);
            log.error(
                    "Deployment table run failed. tableRunId={} table={} errorStep={} errorMessage={} durationMs={}",
                    tableRunId,
                    tableRun.getTableName(),
                    errorStep,
                    errorMessage,
                    tableRun.getDurationMs()
            );
        });
    }

    public List<DeploymentTableRunEntity> getTableRuns(UUID deploymentRunId) {
        return repository.findByDeploymentRunIdOrderByStartedAtAsc(deploymentRunId);
    }

    public List<DeploymentTableRunEntity> getDatastreamTableRuns(UUID datastreamId) {
        return repository.findByDatastreamIdOrderByStartedAtDesc(datastreamId);
    }
}
