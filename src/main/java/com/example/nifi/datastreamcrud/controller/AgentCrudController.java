package com.example.nifi.datastreamcrud.controller;

import com.example.nifi.datastreamcrud.entity.StreamNodeTemplateEntity;
import com.example.nifi.datastreamcrud.service.AgentCrudService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentCrudController {

    private final AgentCrudService service;

    public AgentCrudController(AgentCrudService service) {
        this.service = service;
    }

    @GetMapping("/stream-nodes")
    public ResponseEntity<List<StreamNodeTemplateEntity>> listStreamNodes(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "100") int limit) {

        return ResponseEntity.ok(service.listStreamNodes(skip, limit));
    }
}