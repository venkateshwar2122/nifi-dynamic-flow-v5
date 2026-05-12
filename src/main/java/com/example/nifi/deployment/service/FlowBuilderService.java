package com.example.nifi.deployment.service;

import com.example.nifi.config.NiFiProperties;
import com.example.nifi.flow.dto.FlowRequest;
import com.example.nifi.flow.model.FlowContext;
import com.example.nifi.flow.model.FlowTable;
import com.example.nifi.flow.parser.FlowParser;
import com.example.nifi.nifi.client.NiFiClient;
import com.example.nifi.nifi.controllerservice.ControllerServiceManager;
import com.example.nifi.nifi.tracking.NifiResourceTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FlowBuilderService {

    private static final Logger log = LoggerFactory.getLogger(FlowBuilderService.class);

    private final NiFiClient client;
    private final ControllerServiceManager controllerServiceManager;
    private final FlowParser parser;
    private final NiFiProperties nifi;
    private final NifiResourceTrackingService trackingService;
    private final TablePipelineDeploymentService tablePipelineDeploymentService;

    public FlowBuilderService(
            NiFiClient client,
            ControllerServiceManager controllerServiceManager,
            FlowParser parser,
            NiFiProperties nifi,
            NifiResourceTrackingService trackingService,
            TablePipelineDeploymentService tablePipelineDeploymentService
    ) {
        this.client = client;
        this.controllerServiceManager = controllerServiceManager;
        this.parser = parser;
        this.nifi = nifi;
        this.trackingService = trackingService;
        this.tablePipelineDeploymentService = tablePipelineDeploymentService;
    }

    public String buildFlow(FlowRequest request) {

        String traceId = UUID.randomUUID().toString();
        log.info("==============================================");
        log.info("[{}] START MULTI-TABLE FLOW BUILD", traceId);

        try {
            log.info("[{}] Step 1: Parsing flow request", traceId);
            FlowContext ctx = parser.parse(request);
            UUID datastreamId = request.getDatastreamId();

            log.info("[{}] Step 2: Getting NiFi token", traceId);
            String token = client.getToken();

            log.info("[{}] Step 3: Creating parent process group", traceId);
            String parentProcessGroupId = client.createPG(token, nifi.getRootGroupId(), ctx.getFlowName());

            trackingService.saveResource(
                    datastreamId,
                    parentProcessGroupId,
                    "PARENT_PROCESS_GROUP",
                    ctx.getFlowName(),
                    parentProcessGroupId,
                    "PROCESS_GROUP"
            );

            log.info("[{}] Step 4: Creating shared controller services", traceId);
            TablePipelineDeploymentService.SharedControllerServices services =
                    createSharedControllerServices(token, datastreamId, parentProcessGroupId, ctx);

            log.info("[{}] Step 5: Creating {} child table pipeline(s)", traceId, ctx.getTables().size());
            for (FlowTable table : ctx.getTables()) {
                FlowContext tableContext = ctx.forTable(table);
                tablePipelineDeploymentService.deployTablePipeline(
                        token,
                        datastreamId,
                        parentProcessGroupId,
                        tableContext,
                        services
                );
            }

            log.info("[{}] FLOW CREATED SUCCESSFULLY", traceId);
            log.info("==============================================");

            return parentProcessGroupId;

        } catch (Exception e) {
            log.error("[{}] FLOW CREATION FAILED", traceId, e);
            throw new RuntimeException(e);
        }
    }

    private TablePipelineDeploymentService.SharedControllerServices createSharedControllerServices(
            String token,
            UUID datastreamId,
            String parentProcessGroupId,
            FlowContext ctx
    ) {
        String dbcpId = controllerServiceManager.createDbcp(token, parentProcessGroupId, ctx);
        trackingService.saveResource(
                datastreamId,
                parentProcessGroupId,
                "CONTROLLER_SERVICE",
                "SOURCE_DBCP",
                dbcpId,
                "org.apache.nifi.dbcp.DBCPConnectionPool"
        );

        String writerId = controllerServiceManager.createJsonWriter(token, parentProcessGroupId);
        trackingService.saveResource(
                datastreamId,
                parentProcessGroupId,
                "CONTROLLER_SERVICE",
                "JSON_WRITER",
                writerId,
                "org.apache.nifi.json.JsonRecordSetWriter"
        );

        String readerId = controllerServiceManager.createJsonReader(token, parentProcessGroupId);
        trackingService.saveResource(
                datastreamId,
                parentProcessGroupId,
                "CONTROLLER_SERVICE",
                "JSON_READER",
                readerId,
                "org.apache.nifi.json.JsonTreeReader"
        );

        String mongoId = controllerServiceManager.createMongo(token, parentProcessGroupId, ctx);
        trackingService.saveResource(
                datastreamId,
                parentProcessGroupId,
                "CONTROLLER_SERVICE",
                "MONGO_CLIENT",
                mongoId,
                "org.apache.nifi.mongodb.MongoDBControllerService"
        );

        return new TablePipelineDeploymentService.SharedControllerServices(
                dbcpId,
                writerId,
                readerId,
                mongoId
        );
    }
}
