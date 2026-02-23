package com.dtsx.docs.core.runner.tests.snapshots.reducers;

import com.dtsx.docs.core.runner.tests.snapshots.verifier.Snapshot;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public interface SnapshotsReducer {
    Snapshot reduceSnapshots(Map<Snapshot, Set<Path>> snapshots) throws SnapshotReductionException;
}
