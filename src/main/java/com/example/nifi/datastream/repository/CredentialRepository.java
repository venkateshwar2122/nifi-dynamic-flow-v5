package com.example.nifi.datastream.repository;

import com.example.nifi.datastream.entity.CredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CredentialRepository extends JpaRepository<CredentialEntity, UUID> {
}