package com.dtsx.docs.runner;

import com.dtsx.docs.config.VerifierCtx;
import org.approvaltests.core.Options;
import org.approvaltests.inline.InlineOptions;
import org.lambda.functions.Function1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// Verification mode controlling how test output is compared against snapshots using ApprovalTests.
///
/// Modes:
/// - `DEFAULT` - Auto-selects based on number of languages (`NORMAL` for 1, `VERIFY_ONLY` for multiple)
/// - `NORMAL` - Standard ApprovalTests behavior: creates a "diff" file on mismatch
/// - `VERIFY_ONLY` - Read-only mode: compares against approved files but does not create "diff" files
/// - `DRY_RUN` - Prints what would run without executing anything
public enum VerifyMode {
    /// Auto-selects NORMAL for single-language runs, VERIFY_ONLY for multi-language.
    ///
    /// This is the default mode when not explicitly specified.
    DEFAULT,

    /// Compares the snapshot output against approved files on disk, creating a "diff" file if they differ.
    ///
    /// Only works against a single language at a time.
    NORMAL,

    /// Compares the snapshot output against approved files on disk, but does not create a "diff" file on mismatch (read-only mode).
    ///
    /// Intended for multi-language test runs creating multiple diff files in a single run is cumbersome.
    VERIFY_ONLY,

    /// Prints what would run without executing tests or verifying snapshots.
    ///
    /// Useful for debugging test discovery and planning.
    ///
    /// This also stops {@linkplain com.dtsx.docs.builder.fixtures.JSFixture fixtures} from running, but still sets up the {@linkplain com.dtsx.docs.runner.ExecutionEnvironment execution environment}.
    DRY_RUN,

    /// Forces tests to use {@link com.dtsx.docs.runner.strategies.CompilesTestStrategy CompilesTestStrategy}.
    ///
    /// Dependent on the invariant that all snapshot tests can be run as compilation tests.
    COMPILE_ONLY;

    private static final InlineOptions INLINE_OPTIONS = InlineOptions.showCode(false);

    /// Applies verification options based on the mode.
    ///
    /// @param ctx the verifier context
    /// @param approvedFile the path to the approved snapshot file
    /// @return a function that configures ApprovalTests options
    /// @throws TestRunException if mode is invalid for the configuration
    public Function1<Options, Options> applyOptions(VerifierCtx ctx, Path approvedFile) {
        return switch (actual(ctx)) {
            case NORMAL -> {
                if (ctx.languages().size() > 1) {
                    throw new TestRunException("NORMAL verification mode is only supported for single-language test runs. This should've been caught during VerifierCtx initialization.");
                }
                yield (o) -> o;
            }
            case VERIFY_ONLY -> (o) -> {
                if (!Files.exists(approvedFile)) {
                    return o.inline("", INLINE_OPTIONS);
                }

                try {
                    return o.inline(Files.readString(approvedFile), INLINE_OPTIONS);
                } catch (IOException e) {
                    throw new TestRunException("Failed to read example file for VERIFY_ONLY verification mode: " + approvedFile, e);
                }
            };
            case DRY_RUN -> {
                throw new TestRunException("DRY_RUN mode should not apply verification options; it should've skipped verification entirely.");
            }
            case COMPILE_ONLY -> {
                throw new TestRunException("COMPILE_ONLY mode should not apply verification options; it should've used a different test strategy.");
            }
            case DEFAULT -> {
                throw new TestRunException("DEFAULT mode should have been resolved to a concrete mode before applying verification options.");
            }
        };
    }

    public String displayName(VerifierCtx ctx) {
        return actual(ctx).name().toLowerCase().replace("_", " ");
    }

    private VerifyMode actual(VerifierCtx ctx) {
        if (this == DEFAULT) {
            return (ctx.languages().size() > 1) ? VERIFY_ONLY : NORMAL;
        }
        return this;
    }
}
