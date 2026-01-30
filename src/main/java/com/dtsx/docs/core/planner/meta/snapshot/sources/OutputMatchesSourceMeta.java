package com.dtsx.docs.core.planner.meta.snapshot.sources;

import lombok.NonNull;

import java.util.regex.Pattern;

public record OutputMatchesSourceMeta(@NonNull Pattern regex) {}
