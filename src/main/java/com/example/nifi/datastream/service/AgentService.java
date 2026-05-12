package com.example.nifi.datastream.service;

import com.example.nifi.datastream.entity.StreamNodeTemplateEntity;
import com.example.nifi.datastream.repository.StreamNodeTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AgentService {

    private final StreamNodeTemplateRepository repository;

    public AgentService(StreamNodeTemplateRepository repository) {
        this.repository = repository;
    }

    public List<StreamNodeTemplateEntity> listStreamNodes(int skip, int limit) {
        if (skip < 0) {
            skip = 0;
        }

        if (limit <= 0) {
            limit = 100;
        }

        return repository.findAll()
                .stream()
                .sorted(Comparator.comparing(StreamNodeTemplateEntity::getId))
                .skip(skip)
                .limit(limit)
                .toList();
    }
}