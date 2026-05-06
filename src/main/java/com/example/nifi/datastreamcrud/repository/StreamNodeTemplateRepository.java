package com.example.nifi.datastreamcrud.repository;

import com.example.nifi.datastreamcrud.entity.StreamNodeTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamNodeTemplateRepository extends JpaRepository<StreamNodeTemplateEntity, Long> {
}