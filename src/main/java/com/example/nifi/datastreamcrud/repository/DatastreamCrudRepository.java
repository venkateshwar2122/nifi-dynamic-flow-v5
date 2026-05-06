package com.example.nifi.datastreamcrud.repository;

import com.example.nifi.datastreamcrud.entity.DataStreamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DatastreamCrudRepository extends JpaRepository<DataStreamEntity, UUID> {
}