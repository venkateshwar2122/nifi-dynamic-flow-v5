package com.example.nifi.deployment.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeploymentRunRepository extends JpaRepository<DeploymentRunEntity, UUID> {

    List<DeploymentRunEntity> findByDatastreamIdOrderByStartedAtDesc(UUID datastreamId);
}
