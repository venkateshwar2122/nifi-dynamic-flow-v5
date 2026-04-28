package com.example.nifi.service;

import com.example.nifi.dto.FlowContext;
import com.example.nifi.dto.FlowRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FlowParser {

    private static final Logger log = LoggerFactory.getLogger(FlowParser.class);

    public FlowContext parse(FlowRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.info("==============================================");
        log.info("🔍 [{}] START Flow Parsing", traceId);

        try {

            // =========================
            // STEP 0: VALIDATION
            // =========================
            log.info("[{}] Step 0: Validating request", traceId);

            if (request == null) {
                throw new IllegalArgumentException("Request is null");
            }

            if (request.getStreamEdges() == null || request.getStreamEdges().isEmpty()) {
                throw new IllegalArgumentException("No edges found in request");
            }

            if (request.getStreamNodes() == null || request.getStreamNodes().isEmpty()) {
                throw new IllegalArgumentException("No nodes found in request");
            }

            FlowContext ctx = new FlowContext();
            ctx.setFlowName(request.getDatastreamName());

            log.info("[{}] Flow Name: {}", traceId, ctx.getFlowName());

            // =========================
            // STEP 1: FIND EDGE
            // =========================
            log.info("[{}] Step 1: Finding source and destination from edges", traceId);

            Map<String, Object> edge = request.getStreamEdges().get(0);

            String sourceId = (String) edge.get("source");
            String destId = (String) edge.get("target");

            log.info("[{}] Source ID: {}", traceId, sourceId);
            log.info("[{}] Destination ID: {}", traceId, destId);

            if (sourceId == null || destId == null) {
                throw new IllegalArgumentException("Invalid edge: source/target missing");
            }

            // =========================
            // STEP 2: FIND NODES
            // =========================
            log.info("[{}] Step 2: Locating source and destination nodes", traceId);

            Map<String, Object> sourceNode = null;
            Map<String, Object> destNode = null;

            for (Map<String, Object> node : request.getStreamNodes()) {

                if (sourceId.equals(node.get("id"))) {
                    sourceNode = (Map<String, Object>) node.get("data");
                }

                if (destId.equals(node.get("id"))) {
                    destNode = (Map<String, Object>) node.get("data");
                }
            }

            if (sourceNode == null) {
                throw new IllegalArgumentException("Source node not found for ID: " + sourceId);
            }

            if (destNode == null) {
                throw new IllegalArgumentException("Destination node not found for ID: " + destId);
            }

            log.info("[{}] ✅ Nodes identified successfully", traceId);

            // =========================
            // STEP 3: SOURCE CONFIG
            // =========================
            log.info("[{}] Step 3: Extracting source configuration", traceId);

            Map<String, Object> sourceConfig =
                    (Map<String, Object>) ((Map<String, Object>) sourceNode.get("connection")).get("config");

            if (sourceConfig == null) {
                throw new IllegalArgumentException("Source connection config missing");
            }

            ctx.setSourceDbType((String) sourceConfig.get("dbType"));
            ctx.setSourceHost((String) sourceConfig.get("host"));
            ctx.setSourcePort((Integer) sourceConfig.get("port"));
            ctx.setSourceDatabase((String) sourceConfig.get("database"));
            ctx.setSourceUser((String) sourceConfig.get("username"));
            ctx.setSourcePassword((String) sourceConfig.get("password"));

            log.info("[{}] Source DB: {} | {}:{}", traceId,
                    ctx.getSourceDbType(), ctx.getSourceHost(), ctx.getSourcePort());

            // =========================
            // STEP 4: DEST CONFIG
            // =========================
            log.info("[{}] Step 4: Extracting destination configuration", traceId);

            Map<String, Object> destConfig =
                    (Map<String, Object>) ((Map<String, Object>) destNode.get("connection")).get("config");

            if (destConfig == null) {
                throw new IllegalArgumentException("Destination connection config missing");
            }

            ctx.setDestinationDbType((String) destConfig.get("dbType"));
            ctx.setDestinationHost((String) destConfig.get("host"));
            ctx.setDestinationPort((Integer) destConfig.get("port"));
            ctx.setDestinationDatabase((String) destConfig.get("database"));
            ctx.setDestinationUser((String) destConfig.get("username"));
            ctx.setDestinationPassword((String) destConfig.get("password"));

            log.info("[{}] Destination DB: {} | {}:{}", traceId,
                    ctx.getDestinationDbType(), ctx.getDestinationHost(), ctx.getDestinationPort());

            // =========================
            // STEP 5: TABLE EXTRACTION
            // =========================
            log.info("[{}] Step 5: Extracting table from source schema", traceId);

            List<Map<String, Object>> schema =
                    (List<Map<String, Object>>) sourceNode.get("schema");

            if (schema == null || schema.isEmpty()) {
                throw new IllegalArgumentException("Schema is empty in source node");
            }

            Map<String, Object> table =
                    ((List<Map<String, Object>>) schema.get(0).get("tables")).get(0);

            String tableName = (String) table.get("tableName");

            if (tableName == null) {
                throw new IllegalArgumentException("Table name missing");
            }

            ctx.setTableName(tableName);

            log.info("[{}] Table selected: {}", traceId, tableName);

            // =========================
            // STEP 6: VALIDATE ID COLUMN
            // =========================
            log.info("[{}] Step 6: Validating primary key 'id'", traceId);

            List<Map<String, Object>> columns =
                    (List<Map<String, Object>>) table.get("columns");

            boolean hasId = false;

            for (Map<String, Object> col : columns) {
                if ("id".equalsIgnoreCase((String) col.get("columnName"))) {
                    hasId = true;
                    break;
                }
            }

            if (!hasId) {
                throw new IllegalArgumentException("No 'id' column in table: " + tableName);
            }

            log.info("[{}] ✅ Primary key validation passed", traceId);

            // =========================
            // STEP 7: COLLECTION
            // =========================
            if ("mongodb".equalsIgnoreCase(ctx.getDestinationDbType())) {
                ctx.setCollectionName(ctx.getTableName());
                log.info("[{}] Mongo collection set: {}", traceId, ctx.getCollectionName());
            }

            log.info("[{}] ✅ Flow parsing completed successfully", traceId);
            log.info("==============================================");

            return ctx;

        } catch (IllegalArgumentException e) {
            log.error("❌ [{}] Validation Error in FlowParser", traceId, e);
            throw e;

        } catch (Exception e) {
            log.error("❌ [{}] Unexpected Error in FlowParser", traceId, e);
            throw new RuntimeException("Flow parsing failed: " + e.getMessage(), e);
        }
    }
}