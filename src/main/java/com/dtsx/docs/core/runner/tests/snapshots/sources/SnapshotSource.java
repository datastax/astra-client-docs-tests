package com.dtsx.docs.core.runner.tests.snapshots.sources;

import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.core.runner.tests.snapshots.verifier.SnapshotVerifier;
import lombok.RequiredArgsConstructor;

/// Represents a pluggable source of snapshots based on the run output, or the database state.
///
/// @see SnapshotSources
/// @see RecordSnapshotSource
/// @see OutputSnapshotSource
@RequiredArgsConstructor
public abstract class SnapshotSource implements Comparable<SnapshotSource> {
    protected final SnapshotSources enumRep;

    /// Returns just a string containing the snapshot for this source.
    /// Any header/footer or delimiting information is added by the caller.
    ///
    /// @see SnapshotVerifier
    public abstract String mkSnapshot(TestCtx ctx, RunResult res, Placeholders placeholders);

    public String name() {
        return enumRep.name();
    }

    @Override
    public int compareTo(SnapshotSource other) {
        return this.enumRep.compareTo(other.enumRep); // ensures snapshot source ordering is always deterministic
    }
}
