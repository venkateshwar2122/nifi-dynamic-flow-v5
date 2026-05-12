package com.example.nifi.datastream.service;

import com.example.nifi.datastream.dto.CredentialCreateRequest;
import com.example.nifi.datastream.dto.CredentialUpdateRequest;
import com.example.nifi.datastream.entity.CredentialEntity;
import com.example.nifi.datastream.repository.CredentialRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CredentialService {

    private final CredentialRepository repository;

    public CredentialService(CredentialRepository repository) {
        this.repository = repository;
    }

    public List<CredentialEntity> listCredentials(int skip, int limit) {
        if (skip < 0) {
            skip = 0;
        }

        if (limit <= 0) {
            limit = 100;
        }

        return repository.findAll()
                .stream()
                .sorted(Comparator.comparing(CredentialEntity::getCreatedAt).reversed())
                .skip(skip)
                .limit(limit)
                .toList();
    }

    public CredentialEntity createCredential(CredentialCreateRequest request) {
        if (request.getCredentialName() == null || request.getCredentialName().isBlank()) {
            throw new IllegalArgumentException("credentialName is required");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("password is required");
        }

        CredentialEntity entity = new CredentialEntity();

        entity.setCredentialName(request.getCredentialName());
        entity.setDbType(request.getDbType());
        entity.setHost(request.getHost());
        entity.setPort(request.getPort());
        entity.setDatabaseName(request.getDatabaseName());
        entity.setUsername(request.getUsername());
        entity.setPassword(request.getPassword());
        entity.setCreatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "admin");
        entity.setUpdatedBy(request.getCreatedBy() != null ? request.getCreatedBy() : "admin");

        return repository.save(entity);
    }

    public CredentialEntity getCredentialById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credential not found with id: " + id));
    }

    public CredentialEntity updateCredential(UUID id, CredentialUpdateRequest request) {
        CredentialEntity entity = getCredentialById(id);

        if (request.getCredentialName() != null) {
            entity.setCredentialName(request.getCredentialName());
        }

        if (request.getDbType() != null) {
            entity.setDbType(request.getDbType());
        }

        if (request.getHost() != null) {
            entity.setHost(request.getHost());
        }

        if (request.getPort() != null) {
            entity.setPort(request.getPort());
        }

        if (request.getDatabaseName() != null) {
            entity.setDatabaseName(request.getDatabaseName());
        }

        if (request.getUsername() != null) {
            entity.setUsername(request.getUsername());
        }

        if (request.getPassword() != null) {
            entity.setPassword(request.getPassword());
        }

        if (request.getUpdatedBy() != null) {
            entity.setUpdatedBy(request.getUpdatedBy());
        }

        return repository.save(entity);
    }

    public void deleteCredential(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Credential not found with id: " + id);
        }

        repository.deleteById(id);
    }
}