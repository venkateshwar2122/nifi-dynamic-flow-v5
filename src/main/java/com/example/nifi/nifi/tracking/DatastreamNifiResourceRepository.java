package com.example.nifi.nifi.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatastreamNifiResourceRepository
        extends JpaRepository<DatastreamNifiResourceEntity, UUID> {

    List<DatastreamNifiResourceEntity> findByDatastreamId(UUID datastreamId);

    List<DatastreamNifiResourceEntity> findByDatastreamIdOrderByCreatedAtAsc(UUID datastreamId);

    Optional<DatastreamNifiResourceEntity> findFirstByDatastreamIdAndResourceId(UUID datastreamId, String resourceId);

    void deleteByDatastreamId(UUID datastreamId);
}
