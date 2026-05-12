package com.example.nifi.datastream.controller;

import com.example.nifi.datastream.entity.StreamNodeTemplateEntity;
import com.example.nifi.datastream.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentService service;

    public AgentController(AgentService service) {
        this.service = service;
    }

    @GetMapping("/stream-nodes")
    public ResponseEntity<List<StreamNodeTemplateEntity>> listStreamNodes(
            @RequestParam(defaultValue = "0") int skip,
            @RequestParam(defaultValue = "100") int limit) {

        return ResponseEntity.ok(service.listStreamNodes(skip, limit));
    }
}