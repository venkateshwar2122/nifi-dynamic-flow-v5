package com.example.nifi.datastream.dto;

import java.util.UUID;

public class DatastreamUpdateWithIdRequest extends DatastreamUpdateRequest {

    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}