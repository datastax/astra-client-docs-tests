package com.dtsx.docs.runner.verifier;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.ExampleResultNamer;
import com.dtsx.docs.runner.TestResults.TestOutcome;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestRunner;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.core.Scrubber;
import org.approvaltests.scrubbers.GuidScrubber;
import org.approvaltests.scrubbers.MultiScrubber;
import org.approvaltests.scrubbers.RegExScrubber;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.dtsx.docs.runner.verifier.VerifyMode.DRY_RUN;

/// Verifies test outputs against snapshots using ApprovalTests.
///
/// Snapshots are stored in the `./snapshots` directory by default, with {@linkplain com.dtsx.docs.runner.snapshots.SnapshotSources generation}
/// customizable in the {@link com.dtsx.docs.builder.MetaYml meta.yml} file, and {@linkplain VerifyMode verification mode}
/// configurable via {@linkplain com.dtsx.docs.config.VerifierArgs CLI flags/environment variables}.
///
/// Steps:
/// 1. Checks if it's a dry run; if so, returns {@linkplain TestOutcome.DryPassed a faux result} immediately.
/// 2. Creates the snapshot string based on the {@linkplain com.dtsx.docs.runner.snapshots.SnapshotSources snapshot sources} defined in the test root.
/// 3. Creates the options for ApprovalTests.
///    - This includes the {@link ExampleResultNamer} and {@link TestVerifier#SCRUBBER SCRUBBER}
///    - This also applies any additional options based on the {@link VerifyMode}
/// 4. Uses ApprovalTests to verify the snapshot against the stored snapshot file.
/// 5. Returns the appropriate {@link TestOutcome TestOutcome} based on whether the verification passed or not.
///
/// Snapshots are ordered stably and are of the format:
/// ```
/// ---source1---
/// <snapshot content from source 1>
/// ---source2---
/// <snapshot content from source 2>
/// ...
/// ```
///
/// @see TestRunner
/// @see VerifyMode
/// @see TestResults
@RequiredArgsConstructor
public class TestVerifier {
    /// Scrubber to clean dynamic data from snapshots before verification, replacing them with incrementing placeholders.
    ///
    /// Currently, includes:
    ///   - {@link GuidScrubber}: Scrubs UUIDs
    ///   - {@link $DateScrubber $DateScrubber}: Scrubs collection `$date`s
    public static final Scrubber SCRUBBER = new MultiScrubber(List.of(
        new GuidScrubber(),
        new $DateScrubber()
    ));

    private final VerifierCtx ctx;

    public TestOutcome verify(ClientLanguage language, TestRoot testRoot, Supplier<RunResult> result) {
        if (ctx.verifyMode() == DRY_RUN) {
            return TestOutcome.DryPassed.INSTANCE;
        }

        val snapshot = mkSnapshot(testRoot, result.get());
        val outcome = verifySnapshot(language, testRoot, snapshot);

        CliLogger.result(testRoot, outcome, snapshot);
        return outcome;
    }

    private String mkSnapshot(TestRoot testRoot, RunResult result) {
        val sb = new StringBuilder();

        for (val source : testRoot.snapshotSources()) {
            val snapshot = source.mkSnapshot(ctx, result);
            sb.append("---").append(source.name().toLowerCase()).append("---\n");
            sb.append(snapshot).append("\n");
        }

        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    private TestOutcome verifySnapshot(ClientLanguage language, TestRoot testRoot, String snapshot) {
        val namer = mkNamer(language, testRoot);
        val options = mkApprovalOptions(namer);

        try {
            Approvals.verify(snapshot, options);
            return TestOutcome.Passed.INSTANCE;
        } catch (Error e) {
            val expectedPath = Optional.of(namer.getApprovedFile(".txt").toPath())
                .filter(Files::exists);

            return new TestOutcome.Failed(expectedPath);
        }
    }

    private Options mkApprovalOptions(ExampleResultNamer namer) {
        return new Options()
            .forFile().withNamer(namer)
            .withScrubber(SCRUBBER)
            .withReporter((_, _) -> true)
            .and(ctx.verifyMode().applyOptions(ctx, namer.getApprovedFile(".txt").toPath()));
    }

    private ExampleResultNamer mkNamer(ClientLanguage language, TestRoot testRoot) {
        return new ExampleResultNamer(ctx, language, testRoot);
    }

    private static class $DateScrubber extends RegExScrubber {
        public $DateScrubber() {
            super(Pattern.compile("\\{\\s*\"\\$date\"\\s*:\\s*\\d+\\s*}"), n -> "{ \"$date\" : \"date_" + n + "\" }");
        }
    }
}
