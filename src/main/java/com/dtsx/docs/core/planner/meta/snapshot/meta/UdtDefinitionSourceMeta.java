package com.dtsx.docs.core.planner.meta.snapshot.meta;

import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record UdtDefinitionSourceMeta(
    @NonNull List<String> types,
    @NonNull Optional<String> keyspace
) implements WithKeyspace {}
