package com.example.nifi.service;

import com.example.nifi.client.NiFiClient;
import com.example.nifi.dto.FlowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ProcessorManager {

    private static final Logger log = LoggerFactory.getLogger(ProcessorManager.class);

    private final NiFiClient client;

    public ProcessorManager(NiFiClient client) {
        this.client = client;
    }

    public String getMainRelationship(String type) {

        if (type.contains("QueryDatabaseTableRecord")) return "success";
        if (type.contains("PutMongoRecord")) return "success";

        // future processors
        if (type.contains("PutDatabaseRecord")) return "success";

        throw new RuntimeException("No main relationship defined for: " + type);
    }

    public static class ProcessorInfo {
        private final String id;
        private final String type;

        public ProcessorInfo(String id, String type) {
            this.id = id;
            this.type = type;
        }

        public String getId() { return id; }
        public String getType() { return type; }
    }

    // ================= SOURCE =================
    public ProcessorInfo createSourceProcessor(String token, String pgId,
                                               String dbcpId, String writerId,
                                               FlowContext ctx) {

        if ("mysql".equalsIgnoreCase(ctx.getSourceDbType())) {
            return createQueryProcessor(token, pgId, dbcpId, writerId, ctx);
        }

        throw new RuntimeException("Unsupported source: " + ctx.getSourceDbType());
    }

    // ================= DEST =================
    public ProcessorInfo createDestinationProcessor(String token, String pgId,
                                                    String mongoId, String readerId,
                                                    FlowContext ctx) {

        if ("mongodb".equalsIgnoreCase(ctx.getDestinationDbType())) {
            return createPutMongoProcessor(token, pgId, mongoId, readerId, ctx);
        }

        throw new RuntimeException("Unsupported destination: " + ctx.getDestinationDbType());
    }

    // ================= MYSQL QUERY =================
    private ProcessorInfo createQueryProcessor(String token, String pgId,
                                               String dbcpId, String writerId,
                                               FlowContext ctx) {

        String type = "org.apache.nifi.processors.standard.QueryDatabaseTableRecord";

        log.info("⚙️ Creating Query Processor");

        String id = client.createProcessor(token, pgId, type);
        int version = client.getVersion(token, id, "processors");

        Map<String, Object> props = Map.of(
                "Database Connection Pooling Service", dbcpId,
                "Table Name", ctx.getTableName(),
                "Record Writer", writerId,
                "Maximum-value Columns", "id"
        );

        List<String> rels = client.getRelationships(token, id);

        // auto-terminate ALL relationships initially
        client.updateProcessorFull(token, id, version, props, "5 sec", rels);

        return new ProcessorInfo(id, type);
    }

    // ================= PUT MONGO =================
    private ProcessorInfo createPutMongoProcessor(String token, String pgId,
                                                  String mongoId, String readerId,
                                                  FlowContext ctx) {

        String type = "org.apache.nifi.processors.mongodb.PutMongoRecord";

        log.info("⚙️ Creating Mongo Processor");

        String id = client.createProcessor(token, pgId, type);
        int version = client.getVersion(token, id, "processors");

        Map<String, Object> props = Map.of(
                "Client Service", mongoId,
                "Mongo Database Name", ctx.getDestinationDatabase(),
                "Mongo Collection Name", ctx.getCollectionName(),
                "Record Reader", readerId,
                "Update Key Fields", "id"
        );

        List<String> rels = client.getRelationships(token, id);

        client.updateProcessorFull(token, id, version, props, "0 sec", rels);

        return new ProcessorInfo(id, type);
    }
}