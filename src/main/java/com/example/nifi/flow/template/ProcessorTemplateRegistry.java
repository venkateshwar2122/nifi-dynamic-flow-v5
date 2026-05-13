package com.example.nifi.flow.template;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProcessorTemplateRegistry {

    private final Map<String, ProcessorTemplate> templates = Map.of(
            "source:mysql", new ProcessorTemplate(
                    "source:mysql",
                    ProcessorRole.SOURCE,
                    "org.apache.nifi.processors.standard.QueryDatabaseTableRecord",
                    "success",
                    "5 sec",
                    Map.of(
                            "dbcp", "Database Connection Pooling Service",
                            "writer", "qdbtr-record-writer"
                    )
            ),
            "destination:mongodb", new ProcessorTemplate(
                    "destination:mongodb",
                    ProcessorRole.DESTINATION,
                    "org.apache.nifi.processors.mongodb.PutMongoRecord",
                    "success",
                    "0 sec",
                    Map.of(
                            "mongo", "mongo-client-service",
                            "reader", "record-reader"
                    )
            )
    );

    public ProcessorTemplate source(String dbType) {
        return require("source:" + normalize(dbType));
    }

    public ProcessorTemplate destination(String dbType) {
        return require("destination:" + normalize(dbType));
    }

    private ProcessorTemplate require(String key) {
        ProcessorTemplate template = templates.get(key);

        if (template == null) {
            throw new IllegalArgumentException("Unsupported processor template: " + key);
        }

        return template;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Processor template key cannot be blank");
        }

        return value.trim().toLowerCase();
    }
}
