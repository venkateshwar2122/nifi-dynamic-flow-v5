package com.example.nifi.datastreamcrud.service;

import com.example.nifi.datastreamcrud.entity.StreamNodeTemplateEntity;
import com.example.nifi.datastreamcrud.repository.StreamNodeTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class AgentCrudService {

    private final StreamNodeTemplateRepository repository;

    public AgentCrudService(StreamNodeTemplateRepository repository) {
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