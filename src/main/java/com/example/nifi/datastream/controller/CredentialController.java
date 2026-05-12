package com.example.nifi.datastream.controller;

import com.example.nifi.datastream.dto.CredentialCreateRequest;
import com.example.nifi.datastream.dto.CredentialUpdateRequest;
import com.example.nifi.datastream.entity.CredentialEntity;
import com.example.nifi.datastream.service.CredentialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credentials")
public class CredentialController {

    private final CredentialService service;

    public CredentialController(CredentialService service) {
        this.service = service;
    }

    @GetMapping("/")
    public ResponseEntity<List<CredentialEntity>> listCredentials(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "100") int limit) {

        return ResponseEntity.ok(service.listCredentials(skip, limit));
    }

    @PostMapping("/")
    public ResponseEntity<CredentialEntity> createCredential(
            @RequestBody CredentialCreateRequest request) {

        return ResponseEntity.ok(service.createCredential(request));
    }

    @GetMapping("/{credentialId}")
    public ResponseEntity<CredentialEntity> getCredentialById(
            @PathVariable UUID credentialId) {

        return ResponseEntity.ok(service.getCredentialById(credentialId));
    }

    @PutMapping("/{credentialId}")
    public ResponseEntity<CredentialEntity> updateCredential(
            @PathVariable UUID credentialId,
            @RequestBody CredentialUpdateRequest request) {

        return ResponseEntity.ok(service.updateCredential(credentialId, request));
    }

    @DeleteMapping("/{credentialId}")
    public ResponseEntity<String> deleteCredential(
            @PathVariable UUID credentialId) {

        service.deleteCredential(credentialId);
        return ResponseEntity.ok("Credential deleted successfully");
    }
}