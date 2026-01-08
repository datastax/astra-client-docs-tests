package com.dtsx.docs.runner.snapshots;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class SnapshotSource implements Comparable<SnapshotSource> {
    protected final SnapshotSources enumRep;

    public abstract String mkSnapshot(VerifierCtx ctx, RunResult res);

    public String name() {
        return enumRep.name();
    }

    @Override
    public int compareTo(SnapshotSource other) {
        return this.enumRep.compareTo(other.enumRep);
    }
}
