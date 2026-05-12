package com.example.nifi.datastream.repository;

import com.example.nifi.datastream.entity.StreamNodeTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamNodeTemplateRepository extends JpaRepository<StreamNodeTemplateEntity, Long> {
}