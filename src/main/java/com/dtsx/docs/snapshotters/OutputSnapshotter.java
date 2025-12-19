package com.dtsx.docs.snapshotters;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.ExternalRunners.RunResult;
import org.graalvm.collections.Pair;

import static com.dtsx.docs.snapshotters.SnapshotType.OUTPUT;

public enum OutputSnapshotter implements Snapshotter {
    INSTANCE;

    @Override
    public Pair<SnapshotType, String> mkSnapshot(VerifierConfig cfg, RunResult res) {
        return Pair.create(OUTPUT, res.stdout() + res.stderr());
    }
}
