package com.example.nifi.nifi.controllerservice;

import com.example.nifi.nifi.client.NiFiClient;
import com.example.nifi.nifi.client.NiFiControllerServiceVerifier;
import com.example.nifi.config.FlowProperties;
import com.example.nifi.flow.model.FlowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
            log.info("Applying Controller Service properties. id={} type={} properties={}",
                    id,
                    type,
                    sanitizeProperties(props)
            );
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
        log.info("Preparing DBCP controller service. jdbcUrl={} driverClass={} driverLocation={}",
                url,
                flow.getDbcp().getDriverClass(),
                flow.getDbcp().getDriverLocation()
        );

        return setup(token, pgId,
                "org.apache.nifi.dbcp.DBCPConnectionPool",
                Map.of(
                        "Database Connection URL", url,
                        "Database Driver Class Name", flow.getDbcp().getDriverClass(),
                        "Database Driver Location(s)", flow.getDbcp().getDriverLocation(),
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
                Map.of("schema-access-strategy", "infer-schema"));
    }

    public String createMongo(String token, String pgId, FlowContext ctx) {

        String uri = "mongodb://" + ctx.getDestinationHost() + ":" + ctx.getDestinationPort();
        log.info("Preparing Mongo controller service. uri={} database={}",
                uri,
                ctx.getDestinationDatabase()
        );

        Map<String, Object> props = new HashMap<>();
        props.put("mongo-uri", uri);

        if (hasText(ctx.getDestinationUser())) {
            props.put("Database User", ctx.getDestinationUser());
        }

        if (hasText(ctx.getDestinationPassword())) {
            props.put("Password", ctx.getDestinationPassword());
        }

        return setup(token, pgId,
                "org.apache.nifi.mongodb.MongoDBControllerService",
                props);
    }

    private Map<String, Object> sanitizeProperties(Map<String, Object> props) {
        if (props == null || props.isEmpty()) {
            return Map.of();
        }

        return props.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> isSensitive(entry.getKey()) ? "******" : entry.getValue()
                ));
    }

    private boolean isSensitive(String key) {
        String normalized = key == null ? "" : key.toLowerCase();
        return normalized.contains("password") || normalized.contains("token") || normalized.contains("secret");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
