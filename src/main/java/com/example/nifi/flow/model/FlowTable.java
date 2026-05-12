package com.example.nifi.flow.model;

public class FlowTable {

    private final String tableName;
    private final String collectionName;

    public FlowTable(String tableName, String collectionName) {
        this.tableName = tableName;
        this.collectionName = collectionName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getCollectionName() {
        return collectionName;
    }
}
