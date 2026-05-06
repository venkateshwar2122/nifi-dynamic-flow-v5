package com.example.nifi.service;

import com.example.nifi.datastreamcrud.entity.DataStreamEntity;
import com.example.nifi.dto.FlowRequest;
import org.springframework.stereotype.Component;

@Component
public class FlowRequestMapper {

    public FlowRequest fromEntity(DataStreamEntity entity) {

        FlowRequest request = new FlowRequest();

        request.setDatastreamId(entity.getId());
        request.setDatastreamName(entity.getDatastreamName());
        request.setStreamNodes(entity.getStreamNodes());
        request.setStreamEdges(entity.getStreamEdges());

        return request;
    }
}