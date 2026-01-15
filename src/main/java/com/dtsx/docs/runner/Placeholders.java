package com.dtsx.docs.runner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public class Placeholders {
    public static final Placeholders EMPTY = new Placeholders();

    @JsonProperty("CollectionName")
    private Optional<String> collectionName = Optional.empty();

    @JsonProperty("TableName")
    private Optional<String> tableName = Optional.empty();

    @JsonProperty("KeyspaceName")
    private String keyspaceName = "default_keyspace";

    public Placeholders(String collectionName, String tableName, String keyspaceName) {
        this.collectionName = Optional.ofNullable(collectionName);
        this.tableName = Optional.ofNullable(tableName);
        this.keyspaceName = keyspaceName;
    }
}
