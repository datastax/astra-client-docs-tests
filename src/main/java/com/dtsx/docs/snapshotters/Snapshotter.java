package com.dtsx.docs.snapshotters;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.lib.ExternalRunners.RunResult;
import org.graalvm.collections.Pair;

public interface Snapshotter {
    Pair<String, String> mkSnapshot(VerifierConfig cfg, RunResult res);
}
