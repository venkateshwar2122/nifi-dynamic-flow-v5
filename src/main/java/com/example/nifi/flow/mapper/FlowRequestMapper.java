package com.example.nifi.flow.mapper;

import com.example.nifi.datastream.entity.DataStreamEntity;
import com.example.nifi.flow.dto.FlowRequest;
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