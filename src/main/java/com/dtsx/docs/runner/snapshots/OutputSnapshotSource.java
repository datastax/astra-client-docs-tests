package com.dtsx.docs.runner.snapshots;

import com.dtsx.docs.builder.MetaYml;
import com.dtsx.docs.builder.TestPlanException;
import com.dtsx.docs.builder.fixtures.FixtureMetadata;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/// Base class for snapshot sources that capture process output (stdout or stderr).
///
/// Implemented by [StdoutSnapshotSource] and [StderrSnapshotSource].
///
/// Supports an optional `matches` parameter to verify that the captured output matches a given regex pattern to allow
/// for more flexible verification beyond basic scrubbing of ids/timestamps, rather than capturing the entire output.
///
/// Example configuration:
/// ```
/// stdout:                 <- captures whole stdout
/// stderr:
///  matches: ".*ERROR.*"   <- just checks if stderr contains the word "ERROR"
/// ```
///
/// If `matches` is specified, the actual output won't be stored in the snapshot. Instead, the snapshot will indicate
/// whether the output matched the pattern or not. If it didn't match, the full output will
/// be included in the snapshot for debugging purposes.
///
/// For example:
/// ```
/// ---stderr---
/// Matches '.*ERROR.*'
/// ```
///
/// @apiNote It's often a good idea to just capture `stderr` to ensure there's no warnings or errors, and leave the
/// actual verification to other snapshot sources (e.g. [RecordSnapshotSource])
///
/// @see SnapshotSources
/// @see MetaYml
public sealed abstract class OutputSnapshotSource extends SnapshotSource {
    protected @Nullable Pattern pattern;

    public OutputSnapshotSource(Map<String, Object> params, SnapshotSources enumRep) {
        super(enumRep);

        if (params.get("matches") != null) {
            if (params.get("matches") instanceof String matchesStr) {
                this.pattern = Pattern.compile(matchesStr, Pattern.DOTALL);
            } else {
                throw new TestPlanException("The 'matches' parameter must be a String");
            }
        }
    }

    protected abstract String getCapturedOutput(RunResult res);

    @Override
    public String mkSnapshot(VerifierCtx ctx, RunResult res, FixtureMetadata md) {
        val output = getCapturedOutput(res).trim();

        if (pattern != null) {
            if (pattern.matcher(output).matches()) {
                return "Matches '" + pattern.pattern() + "'";
            } else {
                return "Failed to match '" + pattern.pattern() + "':\n\n" + output;
            }
        }

        return output;
    }

    public static List<String> supportedParams() {
        return List.of("matches");
    }

    /// Implementation of [OutputSnapshotSource] that captures stdout
    public static final class StdoutSnapshotSource extends OutputSnapshotSource {
        public StdoutSnapshotSource(Map<String, Object> params, SnapshotSources enumRep) {
            super(params, enumRep);
        }

        @Override
        protected String getCapturedOutput(RunResult res) {
            return res.stdout();
        }
    }

    /// Implementation of [OutputSnapshotSource] that captures stderr
    public static final class StderrSnapshotSource extends OutputSnapshotSource {
        public StderrSnapshotSource(Map<String, Object> params, SnapshotSources enumRep) {
            super(params, enumRep);
        }

        @Override
        protected String getCapturedOutput(RunResult res) {
            return res.stderr();
        }
    }
}
