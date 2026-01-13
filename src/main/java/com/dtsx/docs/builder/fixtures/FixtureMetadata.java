package com.dtsx.docs.builder.fixtures;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public class FixtureMetadata {
    public static final FixtureMetadata EMPTY = new FixtureMetadata();

    @JsonProperty("CollectionName")
    private Optional<String> collectionName = Optional.empty();

    @JsonProperty("TableName")
    private Optional<String> tableName = Optional.empty();

    @JsonProperty("KeyspaceName")
    private String keyspaceName = "default_keyspace";

    public FixtureMetadata(String collectionName, String tableName, String keyspaceName) {
        this.collectionName = Optional.of(collectionName);
        this.tableName = Optional.of(tableName);
        this.keyspaceName = keyspaceName;
    }
}
