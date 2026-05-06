package com.example.nifi.datastreamcrud.controller;

import com.example.nifi.datastreamcrud.dto.DatastreamCreateRequest;
import com.example.nifi.datastreamcrud.dto.DatastreamUpdateRequest;
import com.example.nifi.datastreamcrud.dto.DatastreamUpdateWithIdRequest;
import com.example.nifi.datastreamcrud.dto.DeleteBatchRequest;
import com.example.nifi.datastreamcrud.entity.DataStreamEntity;
import com.example.nifi.datastreamcrud.service.DatastreamCrudService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/datastreams")
public class DatastreamCrudController {

    private final DatastreamCrudService service;

    public DatastreamCrudController(DatastreamCrudService service) {
        this.service = service;
    }

    @GetMapping("/")
    public ResponseEntity<List<DataStreamEntity>> listDatastreams(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "100") int limit) {

        return ResponseEntity.ok(service.listDatastreams(skip, limit));
    }

    @PostMapping("/")
    public ResponseEntity<DataStreamEntity> createDatastream(
            @RequestBody DatastreamCreateRequest request) {

        return ResponseEntity.ok(service.createDatastream(request));
    }

    @GetMapping("/{datastreamId}")
    public ResponseEntity<DataStreamEntity> getDatastreamById(
            @PathVariable UUID datastreamId) {

        return ResponseEntity.ok(service.getDatastreamById(datastreamId));
    }

    @PutMapping("/{datastreamId}")
    public ResponseEntity<DataStreamEntity> updateDatastream(
            @PathVariable UUID datastreamId,
            @RequestBody DatastreamUpdateRequest request) {

        return ResponseEntity.ok(service.updateDatastream(datastreamId, request));
    }

    @PutMapping("/update-workflow")
    public ResponseEntity<DataStreamEntity> updateWorkflow(
            @RequestBody DatastreamUpdateWithIdRequest request) {

        if (request.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }

        return ResponseEntity.ok(service.updateDatastream(request.getId(), request));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> deleteBatch(
            @RequestBody DeleteBatchRequest batchRequest) {

        if (batchRequest == null || batchRequest.getIds() == null || batchRequest.getIds().isEmpty()) {
            throw new IllegalArgumentException("Batch delete requires ids");
        }

        List<String> deletedDatastreamNames = service.deleteBatch(batchRequest.getIds());

        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "message", "Datastreams deleted successfully: " + deletedDatastreamNames,
                        "deletedCount", deletedDatastreamNames.size(),
                        "deletedDatastreamNames", deletedDatastreamNames,
                        "ids", batchRequest.getIds(),
                        "timestamp", OffsetDateTime.now().toString()
                )
        );
    }

    @DeleteMapping("/{datastreamId}")
    public ResponseEntity<Map<String, Object>> deleteDatastream(
            @PathVariable UUID datastreamId) {

        service.deleteDatastream(datastreamId);

        return ResponseEntity.ok(
                Map.of(
                        "status", 200,
                        "message", "Datastream deleted successfully",
                        "id", datastreamId,
                        "timestamp", OffsetDateTime.now().toString()
                )
        );
    }
}