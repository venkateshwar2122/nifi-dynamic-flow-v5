package com.example.nifi.datastreamcrud.repository;

import com.example.nifi.datastreamcrud.entity.CredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CredentialRepository extends JpaRepository<CredentialEntity, UUID> {
}