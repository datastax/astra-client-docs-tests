package com.dtsx.docs.core.planner.meta.snapshot.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;

import java.util.Optional;

public record OutputJsonifySourceMeta(
    @NonNull Optional<String> jq,
    @NonNull @JsonProperty("jq-bash") Optional<String> jqBash
) {}
