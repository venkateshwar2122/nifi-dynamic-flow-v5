package com.example.nifi.service;

import com.example.nifi.client.NiFiClient;
import com.example.nifi.config.NiFiProperties;
import com.example.nifi.dto.FlowContext;
import com.example.nifi.dto.FlowRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FlowBuilderService {

    private static final Logger log = LoggerFactory.getLogger(FlowBuilderService.class);

    private final NiFiClient client;
    private final ControllerServiceManager cs;
    private final ProcessorManager pm;
    private final FlowParser parser;
    private final NiFiProperties nifi;
    private final NifiResourceTrackingService trackingService;

    public FlowBuilderService(
            NiFiClient client,
            ControllerServiceManager cs,
            ProcessorManager pm,
            FlowParser parser,
            NiFiProperties nifi,
            NifiResourceTrackingService trackingService
    ) {
        this.client = client;
        this.cs = cs;
        this.pm = pm;
        this.parser = parser;
        this.nifi = nifi;
        this.trackingService = trackingService;
    }

    public String buildFlow(FlowRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.info("==============================================");
        log.info("🚀 [{}] START FLOW BUILD", traceId);

        try {

            // STEP 1: PARSE and get ctx
            log.info("[{}] Step 1: Parsing JSON", traceId);
            FlowContext ctx = parser.parse(request);
            UUID datastreamId = request.getDatastreamId();

            // STEP 2: TOKEN
            log.info("[{}] Step 2: Getting NiFi token", traceId);
            String token = client.getToken();

            // STEP 3: PG
            log.info("[{}] Step 3: Creating Process Group", traceId);
            String pgId = client.createPG(token, nifi.getRootGroupId(), ctx.getFlowName());

            trackingService.saveResource(
                    datastreamId,
                    pgId,
                    "PROCESS_GROUP",
                    ctx.getFlowName(),
                    pgId,
                    "PROCESS_GROUP"
            );

            // STEP 4: SERVICES
            log.info("[{}] Step 4: Creating Controller Services", traceId);
            String dbcpId = cs.createDbcp(token, pgId, ctx);

            trackingService.saveResource(
                    datastreamId,
                    pgId,
                    "CONTROLLER_SERVICE",
                    "SOURCE_DBCP",
                    dbcpId,
                    "org.apache.nifi.dbcp.DBCPConnectionPool"
            );
            String writerId = cs.createJsonWriter(token, pgId);

            trackingService.saveResource(
                    datastreamId,
                    pgId,
                    "CONTROLLER_SERVICE",
                    "JSON_WRITER",
                    writerId,
                    "org.apache.nifi.json.JsonRecordSetWriter"
            );
            String readerId = cs.createJsonReader(token, pgId);

            trackingService.saveResource(
                    datastreamId,
                    pgId,
                    "CONTROLLER_SERVICE",
                    "JSON_READER",
                    readerId,
                    "org.apache.nifi.json.JsonTreeReader"
            );
            String mongoId = cs.createMongo(token, pgId, ctx);

            trackingService.saveResource(
                    datastreamId,
                    pgId,
                    "CONTROLLER_SERVICE",
                    "MONGO_CLIENT",
                    mongoId,
                    "org.apache.nifi.mongodb.MongoDBControllerService"
            );

            // STEP 5: PROCESSORS
            log.info("[{}] Step 5: Creating Processors", traceId);
            ProcessorManager.ProcessorInfo source =
                    pm.createSourceProcessor(token, pgId, dbcpId, writerId, ctx);

            trackingService.saveResource(
                    datastreamId,
                    pgId,
                    "PROCESSOR",
                    "SOURCE_PROCESSOR",
                    source.getId(),
                    source.getType()
            );

            ProcessorManager.ProcessorInfo dest =
                    pm.createDestinationProcessor(token, pgId, mongoId, readerId, ctx);

            trackingService.saveResource(
                    datastreamId,
                    pgId,
                    "PROCESSOR",
                    "DESTINATION_PROCESSOR",
                    dest.getId(),
                    dest.getType()
            );
            // STEP 6: RELATIONSHIP
            // ================= STEP 6 FIXED =================

            log.info("[{}] Step 6: Handling Relationships", traceId);

// 1. main relationship
            String mainRel = pm.getMainRelationship(source.getType());

// 2. all relationships
            List<String> allRels = client.getRelationships(token, source.getId());

// 3. remove main from auto-terminate
            List<String> autoTerminate = allRels.stream()
                    .filter(r -> !r.equals(mainRel))
                    .toList();

// 4. FIRST UPDATE
            int version1 = client.getVersion(token, source.getId(), "processors");
            client.updateRelationships(token, source.getId(), version1, autoTerminate);

// 5. WAIT (important)
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}

// 6. SECOND UPDATE (force consistency)
            int version2 = client.getVersion(token, source.getId(), "processors");
            client.updateRelationships(token, source.getId(), version2, autoTerminate);

// 7. VERIFY (optional but powerful)
            List<String> verify = client.getRelationships(token, source.getId());
            log.info("[{}] Relationships after update: {}", traceId, verify);

            log.info("[{}] Main relationship '{}' is ready for connection", traceId, mainRel);

            // STEP 7: CONNECT
            log.info("[{}] Step 7: Connecting processors", traceId);
            String connectionId = client.connect(token, pgId, source.getId(), dest.getId(), mainRel);

            trackingService.saveResource(
                    datastreamId,
                    pgId,
                    "CONNECTION",
                    mainRel,
                    connectionId,
                    "CONNECTION"
            );


            // STEP 8: START
            log.info("[{}] Step 8: Starting flow", traceId);
            client.controlProcessGroup(token, pgId, "RUNNING");

            log.info("✅ [{}] FLOW CREATED SUCCESSFULLY", traceId);
            log.info("==============================================");

            return pgId;

        } catch (Exception e) {
            log.error("❌ [{}] FLOW CREATION FAILED", traceId, e);
            throw new RuntimeException(e);
        }
    }
}