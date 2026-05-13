package com.example.nifi.deployment.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentTableRunRepository extends JpaRepository<DeploymentTableRunEntity, UUID> {

    List<DeploymentTableRunEntity> findByDeploymentRunIdOrderByStartedAtAsc(UUID deploymentRunId);

    List<DeploymentTableRunEntity> findByDatastreamIdOrderByStartedAtDesc(UUID datastreamId);

    Optional<DeploymentTableRunEntity> findFirstByDeploymentRunIdAndTableName(UUID deploymentRunId, String tableName);
}
