package com.example.nifi.dto;

public class FlowContext {

    private String flowName;

    // SOURCE
    private String sourceDbType;
    private String sourceHost;
    private int sourcePort;
    private String sourceDatabase;
    private String sourceUser;
    private String sourcePassword;

    // DESTINATION
    private String destinationDbType;
    private String destinationHost;
    private int destinationPort;
    private String destinationDatabase;
    private String destinationUser;
    private String destinationPassword;

    // DATA
    private String tableName;
    private String collectionName;

    // getters & setters

    public String getFlowName() { return flowName; }
    public void setFlowName(String flowName) { this.flowName = flowName; }

    public String getSourceDbType() { return sourceDbType; }
    public void setSourceDbType(String sourceDbType) { this.sourceDbType = sourceDbType; }

    public String getSourceHost() { return sourceHost; }
    public void setSourceHost(String sourceHost) { this.sourceHost = sourceHost; }

    public int getSourcePort() { return sourcePort; }
    public void setSourcePort(int sourcePort) { this.sourcePort = sourcePort; }

    public String getSourceDatabase() { return sourceDatabase; }
    public void setSourceDatabase(String sourceDatabase) { this.sourceDatabase = sourceDatabase; }

    public String getSourceUser() { return sourceUser; }
    public void setSourceUser(String sourceUser) { this.sourceUser = sourceUser; }

    public String getSourcePassword() { return sourcePassword; }
    public void setSourcePassword(String sourcePassword) { this.sourcePassword = sourcePassword; }

    public String getDestinationDbType() { return destinationDbType; }
    public void setDestinationDbType(String destinationDbType) { this.destinationDbType = destinationDbType; }

    public String getDestinationHost() { return destinationHost; }
    public void setDestinationHost(String destinationHost) { this.destinationHost = destinationHost; }

    public int getDestinationPort() { return destinationPort; }
    public void setDestinationPort(int destinationPort) { this.destinationPort = destinationPort; }

    public String getDestinationDatabase() { return destinationDatabase; }
    public void setDestinationDatabase(String destinationDatabase) { this.destinationDatabase = destinationDatabase; }

    public String getDestinationUser() { return destinationUser; }
    public void setDestinationUser(String destinationUser) { this.destinationUser = destinationUser; }

    public String getDestinationPassword() { return destinationPassword; }
    public void setDestinationPassword(String destinationPassword) { this.destinationPassword = destinationPassword; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
}