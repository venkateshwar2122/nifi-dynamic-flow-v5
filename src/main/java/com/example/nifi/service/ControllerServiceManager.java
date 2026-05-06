package com.example.nifi.service;

import com.example.nifi.client.NiFiClient;
import com.example.nifi.client.NiFiControllerServiceVerifier;
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
    private final NiFiControllerServiceVerifier verifier;

    public ControllerServiceManager(
            NiFiClient client,
            FlowProperties flow,
            NiFiControllerServiceVerifier verifier
    ) {
        this.client = client;
        this.flow = flow;
        this.verifier = verifier;
    }

    private String setup(String token, String pgId, String type, Map<String, Object> props) {

        log.info("🔧 Creating Controller Service: {}", type);

        try {
            String id = client.createCS(token, pgId, type);

            int version = client.getVersion(token, id, "controller-services");
            client.updateCS(token, id, version, props);

            if (shouldVerify(type)) {
                log.info("🔎 Verifying Controller Service before enable: {}", id);
                verifier.verify(id);
            }

            int newVersion = client.getVersion(token, id, "controller-services");
            client.enable(token, id, newVersion);

            log.info("✅ Controller Service Enabled: {}", id);
            return id;

        } catch (Exception e) {
            log.error("❌ Controller Service setup failed for type: {}", type, e);
            throw new RuntimeException("Controller Service setup failed: " + type + " | " + e.getMessage(), e);
        }
    }

    private boolean shouldVerify(String type) {
        return type.contains("DBCPConnectionPool")
                || type.contains("MongoDBControllerService");
    }

    public String createDbcp(String token, String pgId, FlowContext ctx) {

        String url = "jdbc:mysql://" + ctx.getSourceHost() + ":"
                + ctx.getSourcePort() + "/" + ctx.getSourceDatabase();

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

    public String createJsonWriter(String token, String pgId) {
        return setup(token, pgId,
                "org.apache.nifi.json.JsonRecordSetWriter",
                Map.of());
    }

    public String createJsonReader(String token, String pgId) {
        return setup(token, pgId,
                "org.apache.nifi.json.JsonTreeReader",
                Map.of("Schema Access Strategy", "infer-schema"));
    }

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