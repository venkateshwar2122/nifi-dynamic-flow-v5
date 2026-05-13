package com.example.nifi.nifi.client;

import com.example.nifi.config.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
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
            log.info("Requesting NiFi access token from {}", url);

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

            String token = res.getBody();
            if (token == null || token.isBlank()) {
                throw new RuntimeException("Token fetch failed: empty NiFi token response");
            }

            log.info("NiFi access token received");
            return token;

        } catch (RestClientResponseException e) {
            log.error(
                    "NiFi token fetch failed. status={} body={}",
                    e.getStatusCode(),
                    safeBody(e.getResponseBodyAsString()),
                    e
            );
            throw new RuntimeException("Token fetch failed: " + e.getStatusCode() + " " + safeBody(e.getResponseBodyAsString()), e);
        } catch (Exception e) {
            log.error("NiFi token fetch failed: {}", e.getMessage(), e);
            throw new RuntimeException("Token fetch failed", e);
        }
    }
    // ================= PROCESS GROUP =================
    public String createPG(String token, String rootId, String name) {
        log.info("Creating NiFi process group. parentGroupId={} name={}", rootId, name);

        Map<String, Object> body = Map.of(
                "revision", Map.of("version", 0),   // ✅ REQUIRED
                "component", Map.of("name", name)
        );

        Map<String, Object> res = post(
                nifi.getBaseUrl() + "/process-groups/" + rootId + "/process-groups",
                token,
                body
        );

        String id = requireId(res, "process group");
        log.info("Created NiFi process group. id={} name={}", id, name);
        return id;
    }

    // ================= PROCESSOR =================
    public String createProcessor(String token, String pgId, String type, ProcessorPosition position) {
        log.info("Creating NiFi processor. processGroupId={} type={} x={} y={}",
                pgId,
                type,
                position.getX(),
                position.getY()
        );

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

        String id = requireId(res, "processor");
        log.info("Created NiFi processor. id={} type={}", id, type);
        return id;
    }

    // ================= CONTROLLER SERVICE =================
    public String createCS(String token, String pgId, String type) {
        log.info("Creating NiFi controller service. processGroupId={} type={}", pgId, type);

        Map<String, Object> body = Map.of(
                "revision", Map.of("version", 0),   // ✅ REQUIRED
                "component", Map.of("type", type)
        );

        Map<String, Object> res = post(
                nifi.getBaseUrl() + "/process-groups/" + pgId + "/controller-services",
                token,
                body
        );

        String id = requireId(res, "controller service");
        log.info("Created NiFi controller service. id={} type={}", id, type);
        return id;
    }

    public void updateCS(String token, String id, int version, Map<String, Object> props) {
        log.info("Updating NiFi controller service. id={} version={} properties={}",
                id,
                version,
                sanitizeProperties(props)
        );

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
        log.info("Enabling NiFi controller service. id={} version={}", id, version);

        put(
                nifi.getBaseUrl() + "/controller-services/" + id + "/run-status",
                token,
                Map.of(
                        "revision", Map.of("version", version),
                        "state", "ENABLED",
                        "id", id   // ✅ REQUIRED
                )
        );

        waitForControllerServiceEnabled(token, id);
    }

    // ================= VERSION =================
    public int getVersion(String token, String id, String type) {
        log.info("Fetching NiFi revision version. type={} id={}", type, id);

        Map<String, Object> res = get(
                nifi.getBaseUrl() + "/" + type + "/" + id,
                token
        );

        int version = (int) castMap(res.get("revision")).get("version");
        log.info("Fetched NiFi revision version. type={} id={} version={}", type, id, version);
        return version;
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
        log.info("Updating NiFi processor. id={} version={} schedule={} properties={} autoTerminate={}",
                id,
                version,
                schedule,
                sanitizeProperties(props),
                rels
        );

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

    public void validateProcessor(String token, String id) {
        for (int attempt = 1; attempt <= 20; attempt++) {
            Map<String, Object> res = get(nifi.getBaseUrl() + "/processors/" + id, token);
            Map<String, Object> component = castMap(res.get("component"));
            String validationStatus = (String) component.get("validationStatus");

            if ("VALID".equalsIgnoreCase(validationStatus)) {
                log.info("NiFi processor is valid. id={}", id);
                return;
            }

            Object validationErrors = component.get("validationErrors");
            log.warn(
                    "NiFi processor not valid yet. id={} attempt={} validationStatus={} validationErrors={}",
                    id,
                    attempt,
                    validationStatus,
                    validationErrors
            );

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while validating NiFi processor: " + id, e);
            }
        }

        Map<String, Object> res = get(nifi.getBaseUrl() + "/processors/" + id, token);
        Map<String, Object> component = castMap(res.get("component"));
        throw new RuntimeException(
                "NiFi processor is invalid. id="
                        + id
                        + " validationStatus="
                        + component.get("validationStatus")
                        + " validationErrors="
                        + component.get("validationErrors")
        );
    }

    public void waitForControllerServiceEnabled(String token, String id) {
        for (int attempt = 1; attempt <= 20; attempt++) {
            Map<String, Object> res = get(nifi.getBaseUrl() + "/controller-services/" + id, token);
            Map<String, Object> component = castMap(res.get("component"));
            Object state = component.get("state");
            Object validationStatus = component.get("validationStatus");

            if ("ENABLED".equalsIgnoreCase(String.valueOf(state))) {
                log.info("NiFi controller service is enabled. id={}", id);
                return;
            }

            if ("INVALID".equalsIgnoreCase(String.valueOf(validationStatus))) {
                throw new RuntimeException(
                        "NiFi controller service is invalid. id="
                                + id
                                + " state="
                                + state
                                + " validationStatus="
                                + validationStatus
                                + " validationErrors="
                                + component.get("validationErrors")
                );
            }

            log.info(
                    "Waiting for NiFi controller service to become enabled. id={} attempt={} state={} validationStatus={}",
                    id,
                    attempt,
                    state,
                    validationStatus
            );

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for NiFi controller service: " + id, e);
            }
        }

        Map<String, Object> res = get(nifi.getBaseUrl() + "/controller-services/" + id, token);
        Map<String, Object> component = castMap(res.get("component"));
        throw new RuntimeException(
                "NiFi controller service did not become enabled. id="
                        + id
                        + " state="
                        + component.get("state")
                        + " validationStatus="
                        + component.get("validationStatus")
                        + " validationErrors="
                        + component.get("validationErrors")
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
        log.info("Changing NiFi process group state. processGroupId={} state={}", pgId, state);

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
        log.info("NiFi HTTP POST {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();
            log.info("NiFi HTTP POST succeeded {}", url);
            return response;
        } catch (RestClientResponseException e) {
            throw nifiHttpException("POST", url, e);
        }
    }

    private Map<String, Object> get(String url, String token) {
        log.info("NiFi HTTP GET {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        try {
            Map<String, Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();
            log.info("NiFi HTTP GET succeeded {}", url);
            return response;
        } catch (RestClientResponseException e) {
            throw nifiHttpException("GET", url, e);
        }
    }

    private void put(String url, String token, Object body) {
        log.info("NiFi HTTP PUT {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            log.info("NiFi HTTP PUT succeeded {}", url);
        } catch (RestClientResponseException e) {
            throw nifiHttpException("PUT", url, e);
        }
    }

    private RuntimeException nifiHttpException(String method, String url, RestClientResponseException e) {
        log.error(
                "NiFi HTTP {} failed. url={} status={} body={}",
                method,
                url,
                e.getStatusCode(),
                safeBody(e.getResponseBodyAsString()),
                e
        );
        return new RuntimeException(
                "NiFi HTTP " + method + " failed for " + url
                        + " | status=" + e.getStatusCode()
                        + " | body=" + safeBody(e.getResponseBodyAsString()),
                e
        );
    }

    private String requireId(Map<String, Object> response, String resourceName) {
        if (response == null) {
            throw new RuntimeException("NiFi returned empty response while creating " + resourceName);
        }

        Object id = response.get("id");
        if (id instanceof String value && !value.isBlank()) {
            return value;
        }

        throw new RuntimeException("NiFi response did not contain id while creating " + resourceName + ": " + response);
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

    private String safeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return body.length() > 1000 ? body.substring(0, 1000) + "...[truncated]" : body;
    }
}
