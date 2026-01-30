package com.dtsx.docs.core.planner.meta.snapshot.sources;

import lombok.NonNull;

import java.util.Map;
import java.util.Optional;

public record RecordSourceMeta(
    @NonNull Optional<Map<String, Object>> filter,
    @NonNull Optional<Map<String, Object>> projection
) {}
