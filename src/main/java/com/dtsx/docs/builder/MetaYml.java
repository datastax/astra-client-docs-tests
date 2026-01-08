package com.dtsx.docs.builder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;

@JsonIgnoreProperties("$schema")
public record MetaYml(
    @NonNull Optional<Boolean> skip,
    @NonNull FixturesConfig fixtures,
    @NonNull SnapshotsConfig snapshots
) {
    public record FixturesConfig(
        @NonNull String base
    ) {}

    public record SnapshotsConfig(
        @NonNull Optional<Boolean> share,
        @NonNull Map<String, Map<String, Object>> sources
    ) {}
}
