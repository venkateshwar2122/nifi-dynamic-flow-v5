package com.example.nifi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "flow")
public class FlowProperties {

    private Scheduling scheduling;
    private Dbcp dbcp;

    public Scheduling getScheduling() {
        return scheduling;
    }

    public void setScheduling(Scheduling scheduling) {
        this.scheduling = scheduling;
    }

    public Dbcp getDbcp() {
        return dbcp;
    }

    public void setDbcp(Dbcp dbcp) {
        this.dbcp = dbcp;
    }

    // ================= INNER CLASSES =================

    public static class Scheduling {
        private String query;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }
    }

    public static class Dbcp {
        private String driverClass;
        private String driverLocation;

        public String getDriverClass() {
            return driverClass;
        }

        public void setDriverClass(String driverClass) {
            this.driverClass = driverClass;
        }

        public String getDriverLocation() {
            return driverLocation;
        }

        public void setDriverLocation(String driverLocation) {
            this.driverLocation = driverLocation;
        }
    }
}