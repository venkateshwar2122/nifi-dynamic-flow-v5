package com.example.nifi.nifi.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DatastreamNifiResourceRepository
        extends JpaRepository<DatastreamNifiResourceEntity, UUID> {

    List<DatastreamNifiResourceEntity> findByDatastreamId(UUID datastreamId);

    void deleteByDatastreamId(UUID datastreamId);
}