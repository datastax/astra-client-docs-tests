package com.dtsx.docs.builder.meta.reps;

import com.dtsx.docs.runner.snapshots.sources.SnapshotSources;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;

import static com.dtsx.docs.builder.meta.reps.BaseMetaYmlRep.TestType.SNAPSHOT;

public record SnapshotTestMetaYmlRep(
    @NonNull String $schema,
    @NonNull TestBlock test,
    @NonNull FixturesConfig fixtures, // TODO this should be optional for snapshot tests w/out a base fixture
    @NonNull SnapshotsConfig snapshots
) implements BaseMetaYmlRep {
    public record FixturesConfig(
        @NonNull String base
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
