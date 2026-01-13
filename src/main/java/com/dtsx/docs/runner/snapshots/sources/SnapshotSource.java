package com.dtsx.docs.runner.snapshots.sources;

import com.dtsx.docs.builder.fixtures.FixtureMetadata;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.snapshots.verifier.SnapshotVerifier;
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
    public abstract String mkSnapshot(VerifierCtx ctx, RunResult res, FixtureMetadata md);

    public String name() {
        return enumRep.name();
    }

    @Override
    public int compareTo(SnapshotSource other) {
        return this.enumRep.compareTo(other.enumRep); // ensures snapshot source ordering is always deterministic
    }
}
