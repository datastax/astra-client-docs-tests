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
    public final Optional<String> collectionName = Optional.empty();

    @JsonProperty("TableName")
    public final Optional<String> tableName = Optional.empty();

    @JsonProperty("KeyspaceName")
    private final String keyspaceName = "default_keyspace";
}
