package com.example.nifi.datastream.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.nifi")
public class DatastreamExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DatastreamExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(HttpMessageNotReadableException e) {
        log.warn("Invalid JSON request: {}", rootMessage(e));
        return error(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid JSON payload: " + rootMessage(e));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "Invalid value for parameter '" + e.getName() + "': " + e.getValue();
        log.warn(message);
        return error(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseError(DataAccessException e) {
        log.error("Database operation failed: {}", rootMessage(e), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Database Error", rootMessage(e));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleRestClientResponse(RestClientResponseException e) {
        log.error(
                "External HTTP call failed. status={} body={}",
                e.getStatusCode(),
                safeBody(e.getResponseBodyAsString()),
                e
        );
        return error(
                HttpStatus.BAD_GATEWAY,
                "External Service Error",
                "External HTTP call failed with status " + e.getStatusCode() + ": " + safeBody(e.getResponseBodyAsString())
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        log.error("Unhandled runtime error: {}", rootMessage(e), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", rootMessage(e));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnknown(Exception e) {
        log.error("Unhandled application error: {}", rootMessage(e), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", rootMessage(e));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private String rootMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : e.getClass().getSimpleName();
    }

    private String safeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return body.length() > 1000 ? body.substring(0, 1000) + "...[truncated]" : body;
    }
}
