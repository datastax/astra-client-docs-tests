package com.dtsx.docs.core.runner.tests.snapshots.sources;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.output.OutputCaptureSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.records.RecordSource;
import com.dtsx.docs.core.runner.tests.snapshots.verifier.SnapshotVerifier;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/// Represents a pluggable source of snapshots based on the run output, or the database state.
///
/// @see RecordSource
/// @see OutputCaptureSource
@RequiredArgsConstructor
public abstract class SnapshotSource implements Comparable<SnapshotSource> {
    @Getter
    protected final String name;

    /// Returns just a string containing the snapshot for this source.
    /// Any header/footer or delimiting information is added by the caller.
    ///
    /// @see SnapshotVerifier
    public abstract String mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, Placeholders placeholders);

    @Override
    public int compareTo(SnapshotSource other) {
        return this.name.compareTo(other.name); // ensures snapshot source ordering is always deterministic
    }
}
