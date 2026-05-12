package com.example.nifi.datastream.repository;

import com.example.nifi.datastream.entity.DataStreamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DatastreamRepository extends JpaRepository<DataStreamEntity, UUID> {
}