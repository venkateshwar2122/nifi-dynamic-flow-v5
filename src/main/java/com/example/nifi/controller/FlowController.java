package com.example.nifi.controller;

import com.example.nifi.dto.FlowRequest;
import com.example.nifi.service.FlowBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flow")
public class FlowController {

    private static final Logger log = LoggerFactory.getLogger(FlowController.class);

    private final FlowBuilderService service;

    public FlowController(FlowBuilderService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createFlow(@RequestBody FlowRequest request) {

        String flowName = request.getDatastreamName();

        log.info("==============================================");
        log.info("🚀 [START] Create Flow API");
        log.info("📌 Flow Name: {}", flowName);

        try {

            // =========================
            // STEP 1: Validate Request
            // =========================
            log.info("Step 1: Validating request");

            if (request == null) {
                log.error("❌ Request body is null");
                return ResponseEntity.badRequest().body("Request body cannot be null");
            }

            if (request.getStreamNodes() == null || request.getStreamEdges() == null) {
                log.error("❌ Invalid payload: Missing nodes or edges");
                return ResponseEntity.badRequest().body("Invalid JSON payload");
            }

            log.info("✅ Request validation successful");

            // =========================
            // STEP 2: Build Flow
            // =========================
            log.info("Step 2: Calling FlowBuilderService");

            String result = service.buildFlow(request);

            log.info("✅ Flow created successfully: {}", flowName);
            log.info("==============================================");

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("❌ Validation Error in Flow: {}", flowName, e);

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Validation Error: " + e.getMessage());

        } catch (RuntimeException e) {
            log.error("❌ Runtime Error while creating flow: {}", flowName, e);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Runtime Error: " + e.getMessage());

        } catch (Exception e) {
            log.error("❌ Unexpected Error while creating flow: {}", flowName, e);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected Error: " + e.getMessage());
        }
    }
}