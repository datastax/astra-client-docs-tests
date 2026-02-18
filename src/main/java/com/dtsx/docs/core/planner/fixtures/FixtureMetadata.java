package com.dtsx.docs.core.planner.fixtures;

import com.dtsx.docs.core.planner.fixtures.BaseFixturePool.FixtureIndex;
import com.dtsx.docs.core.runner.Placeholders;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FixtureMetadata extends Placeholders {
    private FixtureIndex index = FixtureIndex.ZERO;

    private Initialization initialization = Initialization.SEQUENTIAL;

    public enum Initialization {
        PARALLEL,
        SEQUENTIAL
    }

    @JsonCreator
    public FixtureMetadata(
        @JsonProperty("CollectionName") Optional<String> collectionName,
        @JsonProperty("TableName") Optional<String> tableName,
        @JsonProperty("KeyspaceName") Optional<String> keyspaceName,
        @JsonProperty("Initialization") Initialization initialization
    ) {
        super(collectionName, tableName, keyspaceName.orElse("default_keyspace"), Map.of());
        this.initialization = initialization;
    }

    public FixtureMetadata withIndex(FixtureIndex newIndex) { // I hate this but can't really get around it easily since the class is otherwise parsed from JSON
        this.index = newIndex;
        return this;
    }

    public FixtureMetadata withVars(Map<String, String> vars) {
        this.vars = vars;
        return this;
    }

    public static FixtureMetadata emptyForIndex(FixtureIndex index) {
        return new FixtureMetadata(index, Initialization.SEQUENTIAL);
    }
}
