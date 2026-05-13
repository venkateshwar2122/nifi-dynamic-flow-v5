package com.example.nifi.nifi.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatastreamNifiResourceRepository
        extends JpaRepository<DatastreamNifiResourceEntity, UUID> {

    List<DatastreamNifiResourceEntity> findByDatastreamId(UUID datastreamId);

    List<DatastreamNifiResourceEntity> findByDatastreamIdOrderByCreatedAtAsc(UUID datastreamId);

    Optional<DatastreamNifiResourceEntity> findFirstByDatastreamIdAndResourceId(UUID datastreamId, String resourceId);

    @Modifying
    @Query("""
            update DatastreamNifiResourceEntity resource
            set resource.current = false
            where resource.datastreamId = :datastreamId
            """)
    int clearCurrentResources(@Param("datastreamId") UUID datastreamId);

    @Modifying
    @Query("""
            update DatastreamNifiResourceEntity resource
            set resource.current = true
            where resource.datastreamId = :datastreamId
              and resource.deploymentRunId = :deploymentRunId
            """)
    int markRunResourcesCurrent(
            @Param("datastreamId") UUID datastreamId,
            @Param("deploymentRunId") UUID deploymentRunId
    );

    void deleteByDatastreamId(UUID datastreamId);
}
