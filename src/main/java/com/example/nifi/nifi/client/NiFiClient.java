package com.example.nifi.nifi.client;

import com.example.nifi.config.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.example.nifi.flow.model.ProcessorPosition;

@Component
public class NiFiClient {

    private static final Logger log = LoggerFactory.getLogger(NiFiClient.class);

    private final RestTemplate restTemplate;
    private final NiFiProperties nifi;

    public NiFiClient(RestTemplate restTemplate, NiFiProperties nifi) {
        this.restTemplate = restTemplate;
        this.nifi = nifi;
    }

    // ================= SAFE CAST =================
    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object obj) {
        return (List<Map<String, Object>>) obj;
    }

    // ================= TOKEN =================


    public String getToken() {

        try {
            String url = nifi.getBaseUrl().replace("/nifi-api", "") + "/nifi-api/access/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // ✅ Proper form encoding
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("username", nifi.getUsername());
            body.add("password", nifi.getPassword());

            HttpEntity<MultiValueMap<String, String>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> res =
                    restTemplate.postForEntity(url, entity, String.class);

            return res.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Token fetch failed", e);
        }
    }
    // ================= PROCESS GROUP =================
    public String createPG(String token, String rootId, String name) {

        Map<String, Object> body = Map.of(
                "revision", Map.of("version", 0),   // ✅ REQUIRED
                "component", Map.of("name", name)
        );

        Map<String, Object> res = post(
                nifi.getBaseUrl() + "/process-groups/" + rootId + "/process-groups",
                token,
                body
        );

        return (String) res.get("id");
    }

    // ================= PROCESSOR =================
    public String createProcessor(String token, String pgId, String type, ProcessorPosition position) {

        Map<String, Object> body = Map.of(
                "revision", Map.of("version", 0),
                "component", Map.of(
                        "type", type,
                        "position", Map.of(
                                "x", position.getX(),
                                "y", position.getY()
                        )
                )
        );

        Map<String, Object> res = post(
                nifi.getBaseUrl() + "/process-groups/" + pgId + "/processors",
                token,
                body
        );

        return (String) res.get("id");
    }

    // ================= CONTROLLER SERVICE =================
    public String createCS(String token, String pgId, String type) {

        Map<String, Object> body = Map.of(
                "revision", Map.of("version", 0),   // ✅ REQUIRED
                "component", Map.of("type", type)
        );

        Map<String, Object> res = post(
                nifi.getBaseUrl() + "/process-groups/" + pgId + "/controller-services",
                token,
                body
        );

        return (String) res.get("id");
    }

    public void updateCS(String token, String id, int version, Map<String, Object> props) {

        put(
                nifi.getBaseUrl() + "/controller-services/" + id,
                token,
                Map.of(
                        "revision", Map.of("version", version),
                        "component", Map.of(
                                "id", id,              // ✅ CRITICAL FIX
                                "properties", props
                        )
                )
        );
    }

    public void enable(String token, String id, int version) {

        put(
                nifi.getBaseUrl() + "/controller-services/" + id + "/run-status",
                token,
                Map.of(
                        "revision", Map.of("version", version),
                        "state", "ENABLED",
                        "id", id   // ✅ REQUIRED
                )
        );
    }

    // ================= VERSION =================
    public int getVersion(String token, String id, String type) {

        Map<String, Object> res = get(
                nifi.getBaseUrl() + "/" + type + "/" + id,
                token
        );

        return (int) castMap(res.get("revision")).get("version");
    }

    // ================= RELATIONSHIPS =================
    public List<String> getRelationships(String token, String id) {

        for (int i = 0; i < 5; i++) {

            Map<String, Object> res =
                    get(nifi.getBaseUrl() + "/processors/" + id, token);

            Map<String, Object> component = castMap(res.get("component"));

            List<Map<String, Object>> rels =
                    castList(component.get("relationships"));

            if (rels != null && !rels.isEmpty()) {
                return rels.stream()
                        .map(r -> (String) r.get("name"))
                        .toList();
            }

            log.warn("⏳ Waiting for relationships for processor: {}", id);

            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}
        }

        throw new RuntimeException("Failed to fetch relationships for processor: " + id);
    }

    public void updateRelationships(String token, String processorId,
                                    int version, List<String> autoTerminateRels) {

        // 1. GET existing processor
        Map<String, Object> res =
                get(nifi.getBaseUrl() + "/processors/" + processorId, token);

        Map<String, Object> component = castMap(res.get("component"));
        Map<String, Object> config = castMap(component.get("config"));

        if (config == null) {
            throw new RuntimeException("Config not found for processor: " + processorId);
        }

        // 2. UPDATE ONLY autoTerminate
        config.put("autoTerminatedRelationships", autoTerminateRels);

        // 3. SEND FULL CONFIG BACK
        put(
                nifi.getBaseUrl() + "/processors/" + processorId,
                token,
                Map.of(
                        "revision", Map.of("version", version),
                        "component", Map.of(
                                "id", processorId,
                                "config", config   // ✅ FULL CONFIG
                        )
                )
        );

        log.info("✅ Updated relationships for processor {} -> {}", processorId, autoTerminateRels);
    }

    // ================= PROCESSOR FULL UPDATE =================
    public void updateProcessorFull(String token, String id, int version,
                                    Map<String, Object> props,
                                    String schedule,
                                    List<String> rels) {

        put(
                nifi.getBaseUrl() + "/processors/" + id,
                token,
                Map.of(
                        "revision", Map.of("version", version),
                        "component", Map.of(
                                "id", id,   // ✅ ADD THIS
                                "config", Map.of(
                                        "properties", props,
                                        "schedulingPeriod", schedule,
                                        "autoTerminatedRelationships", rels
                                )
                        )
                )
        );
    }

    // ================= CONNECT =================
    public String connect(String token, String pgId,
                          String sourceId, String destId, String rel) {

        Map<String, Object> body = Map.of(
                "revision", Map.of("version", 0),
                "component", Map.of(
                        "parentGroupId", pgId,

                        "source", Map.of(
                                "id", sourceId,
                                "type", "PROCESSOR",
                                "groupId", pgId
                        ),

                        "destination", Map.of(
                                "id", destId,
                                "type", "PROCESSOR",
                                "groupId", pgId
                        ),

                        "selectedRelationships", List.of(rel),

                        // optional but recommended
                        "backPressureObjectThreshold", 10000,
                        "backPressureDataSizeThreshold", "1 GB",
                        "flowFileExpiration", "0 sec",
                        "prioritizers", List.of(),
                        "bends", List.of(),
                        "name", ""
                )
        );

        log.info("🔗 Creating connection: {} -> {} via {}", sourceId, destId, rel);

        Map<String, Object> res = post(
                nifi.getBaseUrl() + "/process-groups/" + pgId + "/connections",
                token,
                body
        );

        return (String) res.get("id");
    }


    // ================= CONTROL FLOW =================
    public void controlProcessGroup(String token, String pgId, String state) {

        int version = getVersion(token, pgId, "process-groups");

        put(
                nifi.getBaseUrl() + "/flow/process-groups/" + pgId,
                token,
                Map.of(
                        "id", pgId,
                        "state", state,
                        "revision", Map.of("version", version)
                )
        );
    }

    // ================= HTTP HELPERS =================
    private Map<String, Object> post(String url, String token, Object body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();
    }

    private Map<String, Object> get(String url, String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        ).getBody();
    }

    private void put(String url, String token, Object body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }
}