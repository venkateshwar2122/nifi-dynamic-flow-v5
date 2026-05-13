package com.example.nifi.nifi.client;

import com.example.nifi.config.NiFiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class NiFiControllerServiceVerifier {

    private static final Logger log = LoggerFactory.getLogger(NiFiControllerServiceVerifier.class);

    private final RestTemplate restTemplate;
    private final NiFiProperties nifi;

    public NiFiControllerServiceVerifier(RestTemplate restTemplate, NiFiProperties nifi) {
        this.restTemplate = restTemplate;
        this.nifi = nifi;
    }

    public void verify(String controllerServiceId) {

        log.info("🔎 Starting controller service verification: {}", controllerServiceId);

        NiFiSession session = loginAndCreateSession();

        String requestId = createVerificationRequest(session, controllerServiceId);

        pollVerificationResult(session, controllerServiceId, requestId);

        log.info("✅ Controller service verification successful: {}", controllerServiceId);
    }

    private NiFiSession loginAndCreateSession() {

        String url = nifi.getBaseUrl().replace("/nifi-api", "") + "/nifi-api/access/token";
        log.info("Creating NiFi verification session. loginUrl={}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", nifi.getUsername());
        body.add("password", nifi.getPassword());

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    String.class
            );
        } catch (RestClientResponseException e) {
            throw verificationHttpException("POST", url, e);
        }

        String jwtToken = response.getBody();

        if (jwtToken == null || jwtToken.isBlank()) {
            throw new RuntimeException("NiFi login failed: empty JWT token");
        }

        List<String> setCookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);

        String requestToken = extractCookieValue(setCookies, "__Secure-Request-Token");

        if (requestToken == null || requestToken.isBlank()) {
            requestToken = UUID.randomUUID().toString();
            log.warn("__Secure-Request-Token cookie not found from login response. Generated request token: {}", requestToken);
        }

        log.info("✅ NiFi verification session ready");

        return new NiFiSession(jwtToken, requestToken);
    }

    private String createVerificationRequest(NiFiSession session, String controllerServiceId) {

        String url = nifi.getBaseUrl()
                + "/controller-services/"
                + controllerServiceId
                + "/config/verification-requests";
        log.info("Creating controller service verification request. controllerServiceId={} url={}",
                controllerServiceId,
                url
        );

        HttpHeaders headers = buildHeaders(session);

        Map<String, Object> body = Map.of(
                "request", Map.of(
                        "properties", Map.of(),
                        "componentId", controllerServiceId,
                        "attributes", Map.of()
                )
        );

        ResponseEntity<Map<String, Object>> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
        } catch (RestClientResponseException e) {
            throw verificationHttpException("POST", url, e);
        }

        Map<String, Object> responseBody = response.getBody();

        if (responseBody == null) {
            throw new RuntimeException("Verification request failed: empty response");
        }

        Map<String, Object> request = castMap(responseBody.get("request"));
        String requestId = (String) request.get("requestId");

        if (requestId == null || requestId.isBlank()) {
            throw new RuntimeException("Verification requestId not found");
        }

        log.info("✅ Verification request created: {}", requestId);

        return requestId;
    }

    private void pollVerificationResult(
            NiFiSession session,
            String controllerServiceId,
            String requestId
    ) {
        String url = nifi.getBaseUrl()
                + "/controller-services/"
                + controllerServiceId
                + "/config/verification-requests/"
                + requestId;
        log.info("Polling controller service verification. controllerServiceId={} requestId={} url={}",
                controllerServiceId,
                requestId,
                url
        );

        HttpHeaders headers = buildHeaders(session);

        Map<String, Object> finalRequest = null;

        for (int i = 0; i < 20; i++) {

            ResponseEntity<Map<String, Object>> response;
            try {
                response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );
            } catch (RestClientResponseException e) {
                throw verificationHttpException("GET", url, e);
            }

            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) {
                throw new RuntimeException("Verification polling failed: empty response");
            }

            finalRequest = castMap(responseBody.get("request"));

            Boolean complete = (Boolean) finalRequest.get("complete");

            if (complete != null && complete) {
                break;
            }

            sleep(500);
        }

        if (finalRequest == null) {
            throw new RuntimeException("Verification failed: no response");
        }

        Boolean complete = (Boolean) finalRequest.get("complete");

        if (complete == null || !complete) {
            throw new RuntimeException("Verification timed out for controller service: " + controllerServiceId);
        }

        List<Map<String, Object>> results = castList(finalRequest.get("results"));

        if (results == null || results.isEmpty()) {
            throw new RuntimeException("Verification completed but no result details returned");
        }

        for (Map<String, Object> result : results) {

            String outcome = (String) result.get("outcome");
            String step = (String) result.get("verificationStepName");
            String explanation = (String) result.get("explanation");

            log.info("🔎 Verification step={} | outcome={} | explanation={}",
                    step,
                    outcome,
                    explanation
            );

            if (!"SUCCESSFUL".equalsIgnoreCase(outcome)) {
                throw new RuntimeException(
                        "Controller service verification failed at step: "
                                + step
                                + " | "
                                + explanation
                );
            }
        }
    }

    private HttpHeaders buildHeaders(NiFiSession session) {

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String base = nifi.getBaseUrl().replace("/nifi-api", "");

        headers.add("Origin", base);
        headers.add("Referer", base + "/nifi/");
        headers.add("request-token", session.requestToken());

        headers.setBearerAuth(session.jwtToken());

        headers.add(
                HttpHeaders.COOKIE,
                "__Secure-Request-Token=" + session.requestToken()
                        + "; __Secure-Authorization-Bearer=" + session.jwtToken()
        );

        return headers;
    }

    private String extractCookieValue(List<String> setCookies, String cookieName) {

        if (setCookies == null || setCookies.isEmpty()) {
            return null;
        }

        for (String cookie : setCookies) {
            if (cookie.startsWith(cookieName + "=")) {
                String value = cookie.substring((cookieName + "=").length());
                int index = value.indexOf(";");

                if (index >= 0) {
                    return value.substring(0, index);
                }

                return value;
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object obj) {
        return (List<Map<String, Object>>) obj;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Verification interrupted", e);
        }
    }

    private record NiFiSession(String jwtToken, String requestToken) {
    }

    private RuntimeException verificationHttpException(String method, String url, RestClientResponseException e) {
        log.error(
                "NiFi verification HTTP {} failed. url={} status={} body={}",
                method,
                url,
                e.getStatusCode(),
                safeBody(e.getResponseBodyAsString()),
                e
        );
        return new RuntimeException(
                "NiFi verification HTTP " + method + " failed for " + url
                        + " | status=" + e.getStatusCode()
                        + " | body=" + safeBody(e.getResponseBodyAsString()),
                e
        );
    }

    private String safeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return body.length() > 1000 ? body.substring(0, 1000) + "...[truncated]" : body;
    }
}
