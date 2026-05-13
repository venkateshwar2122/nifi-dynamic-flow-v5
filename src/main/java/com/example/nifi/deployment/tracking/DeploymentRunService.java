package com.example.nifi.deployment.tracking;

import com.example.nifi.datastream.entity.DataStreamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DeploymentRunService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentRunService.class);

    private final DeploymentRunRepository repository;

    public DeploymentRunService(DeploymentRunRepository repository) {
        this.repository = repository;
    }

    public DeploymentRunEntity startRun(DataStreamEntity datastream, String trigger) {
        DeploymentRunEntity run = new DeploymentRunEntity();
        run.setDatastreamId(datastream.getId());
        run.setDatastreamName(datastream.getDatastreamName());
        run.setDeploymentStatus("STARTED");
        run.setDeploymentTrigger(trigger);
        run.setStartedAt(OffsetDateTime.now());
        run.setCreatedBy(datastream.getUpdatedBy());

        DeploymentRunEntity saved = repository.save(run);
        log.info(
                "Deployment run started. runId={} datastreamId={} trigger={}",
                saved.getId(),
                saved.getDatastreamId(),
                trigger
        );
        return saved;
    }

    public void markRunning(UUID runId) {
        repository.findById(runId).ifPresent(run -> {
            run.setDeploymentStatus("RUNNING");
            repository.save(run);
            log.info("Deployment run marked RUNNING. runId={}", runId);
        });
    }

    public void markSuccess(UUID runId, String processGroupId) {
        repository.findById(runId).ifPresent(run -> {
            run.complete("SUCCESS", processGroupId, null, null);
            repository.save(run);
            log.info(
                    "Deployment run completed successfully. runId={} datastreamId={} processGroupId={} durationMs={}",
                    runId,
                    run.getDatastreamId(),
                    processGroupId,
                    run.getDurationMs()
            );
        });
    }

    public void markFailed(UUID runId, String errorStep, String errorMessage) {
        repository.findById(runId).ifPresent(run -> {
            run.complete("FAILED", run.getProcessGroupId(), errorStep, errorMessage);
            repository.save(run);
            log.error(
                    "Deployment run failed. runId={} datastreamId={} errorStep={} errorMessage={} durationMs={}",
                    runId,
                    run.getDatastreamId(),
                    errorStep,
                    errorMessage,
                    run.getDurationMs()
            );
        });
    }

    public List<DeploymentRunEntity> getRuns(UUID datastreamId) {
        return repository.findByDatastreamIdOrderByStartedAtDesc(datastreamId);
    }
}
