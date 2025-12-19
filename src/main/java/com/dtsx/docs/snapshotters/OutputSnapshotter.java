package com.dtsx.docs.snapshotters;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.ExternalRunners.RunResult;
import org.graalvm.collections.Pair;

public enum OutputSnapshotter implements Snapshotter {
    INSTANCE;

    @Override
    public Pair<String, String> mkSnapshot(VerifierConfig cfg, RunResult res) {
        return Pair.create("stdout", res.stdout() + res.stderr());
    }
}
