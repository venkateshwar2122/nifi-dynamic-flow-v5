package com.example.nifi.service;

import com.example.nifi.client.NiFiClient;
import com.example.nifi.config.FlowProperties;
import com.example.nifi.dto.FlowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ControllerServiceManager {

    private static final Logger log = LoggerFactory.getLogger(ControllerServiceManager.class);

    private final NiFiClient client;
    private final FlowProperties flow;

    public ControllerServiceManager(NiFiClient client, FlowProperties flow) {
        this.client = client;
        this.flow = flow;
    }

    private String setup(String token, String pgId, String type, Map<String, Object> props) {

        log.info("🔧 Creating Controller Service: {}", type);

        try {
            String id = client.createCS(token, pgId, type);

            int version = client.getVersion(token, id, "controller-services");
            client.updateCS(token, id, version, props);

            int newVersion = client.getVersion(token, id, "controller-services");
            client.enable(token, id, newVersion);

            log.info("✅ Controller Service Enabled: {}", id);
            return id;

        } catch (Exception e) {
            log.error("❌ Controller Service creation failed", e);
            throw new RuntimeException(e);
        }
    }

    // ================= DBCP =================
    public String createDbcp(String token, String pgId, FlowContext ctx) {

        String url = "jdbc:mysql://" + ctx.getSourceHost() + ":" +
                ctx.getSourcePort() + "/" + ctx.getSourceDatabase();

        return setup(token, pgId,
                "org.apache.nifi.dbcp.DBCPConnectionPool",
                Map.of(
                        "Database Connection URL", url,
                        "Database Driver Class Name", flow.getDbcp().getDriverClass(),
                        "Database Driver Locations", flow.getDbcp().getDriverLocation(),
                        "Database User", ctx.getSourceUser(),
                        "Password", ctx.getSourcePassword()
                ));
    }

    // ================= JSON =================
    public String createJsonWriter(String token, String pgId) {
        return setup(token, pgId,
                "org.apache.nifi.json.JsonRecordSetWriter", Map.of());
    }

    public String createJsonReader(String token, String pgId) {
        return setup(token, pgId,
                "org.apache.nifi.json.JsonTreeReader",
                Map.of("Schema Access Strategy", "infer-schema"));
    }

    // ================= MONGO =================
    public String createMongo(String token, String pgId, FlowContext ctx) {

        String uri = "mongodb://" + ctx.getDestinationHost() + ":" + ctx.getDestinationPort();

        return setup(token, pgId,
                "org.apache.nifi.mongodb.MongoDBControllerService",
                Map.of(
                        "Mongo URI", uri,
                        "Database User", ctx.getDestinationUser(),
                        "Password", ctx.getDestinationPassword()
                ));
    }
}