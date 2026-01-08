package com.dtsx.docs.runner.snapshots;

import com.dtsx.docs.builder.MetaYml;
import com.dtsx.docs.builder.TestPlanException;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;

import java.util.List;
import java.util.Map;

/// Snapshot source that captures program output (stdout, stderr, or both).
///
/// The `capture` parameter determines what output to capture:
///   - `all` - captures both stdout and stderr sequenced together (default)
///   - `stdout` - captures only stdout
///   - `stderr` - captures only stderr
///
/// Example configuration:
/// ```
/// output:
///   capture: stderr
/// ```
///
/// @apiNote It's often a good idea to just capture `stderr` to ensure there's no warnings or errors, and leave the
/// actual verification to other snapshot sources (e.g. [RecordSnapshotSource])
///
/// @see SnapshotSources
/// @see MetaYml
public class OutputSnapshotSource extends SnapshotSource {
    private enum Capture {
        ALL,
        STDOUT,
        STDERR
    }

    private Capture capture = Capture.ALL;

    public OutputSnapshotSource(Map<String, Object> params, SnapshotSources enumRep) {
        super(enumRep);

        if (params.get("capture") != null) {
            try {
                this.capture = Capture.valueOf(params.get("capture").toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new TestPlanException("Unexpected value for snapshot source output capture: " + params.get("capture") + " (expected one of 'all', 'stdout', 'stderr')");
            }
        }
    }

    @Override
    public String mkSnapshot(VerifierCtx ctx, RunResult res) {
        return switch (capture) {
            case ALL -> res.output();
            case STDOUT -> res.stdout();
            case STDERR -> res.stderr();
        };
    }

    public static List<String> supportedParams() {
        return List.of("capture");
    }
}
