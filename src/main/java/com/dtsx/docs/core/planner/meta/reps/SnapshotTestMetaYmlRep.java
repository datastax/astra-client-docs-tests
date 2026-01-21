package com.dtsx.docs.core.planner.meta.reps;

import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSources;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;

import static com.dtsx.docs.core.planner.meta.reps.BaseMetaYmlRep.TestType.SNAPSHOT;

public record SnapshotTestMetaYmlRep(
    @NonNull String $schema,
    @NonNull TestBlock test,
    @NonNull Optional<FixturesConfig> fixtures,
    @NonNull SnapshotsConfig snapshots
) implements BaseMetaYmlRep {
    public record FixturesConfig(
        @NonNull Optional<String> base
    ) {}

    public record SnapshotsConfig(
        @NonNull Optional<Boolean> share,
        @NonNull Map<SnapshotSources, Map<String, Object>> sources
    ) {}

    @Override
    public TestType expectTestType() {
        return SNAPSHOT;
    }
}
