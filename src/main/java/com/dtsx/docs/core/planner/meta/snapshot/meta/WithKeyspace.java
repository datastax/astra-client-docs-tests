package com.dtsx.docs.core.planner.meta.snapshot.meta;

import java.util.Optional;

public interface WithKeyspace {
    Optional<String> keyspace();

    record Impl(
        Optional<String> keyspace
    ) implements WithKeyspace {}
}
