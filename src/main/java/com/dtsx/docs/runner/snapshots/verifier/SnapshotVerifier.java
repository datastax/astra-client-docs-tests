package com.dtsx.docs.runner.snapshots.verifier;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.FixtureMetadata;
import com.dtsx.docs.builder.meta.reps.SnapshotTestMetaYmlRep;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.TestResults;
import com.dtsx.docs.runner.TestResults.TestOutcome;
import com.dtsx.docs.runner.TestResults.TestOutcome.FailedToVerify;
import com.dtsx.docs.runner.TestRunner;
import com.dtsx.docs.runner.VerifyMode;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.snapshots.sources.SnapshotSource;
import com.dtsx.docs.runner.snapshots.sources.SnapshotSources;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.core.Scrubber;
import org.approvaltests.scrubbers.GuidScrubber;
import org.approvaltests.scrubbers.MultiScrubber;
import org.approvaltests.scrubbers.RegExScrubber;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.dtsx.docs.runner.VerifyMode.DRY_RUN;

/// Verifies test outputs against snapshots using ApprovalTests.
///
/// Snapshots are stored in the `./snapshots` directory by default, with {@linkplain SnapshotSources generation}
/// customizable in the {@link SnapshotTestMetaYmlRep meta.yml} file, and {@linkplain VerifyMode verification mode}
/// configurable via {@linkplain com.dtsx.docs.config.VerifierArgs CLI flags/environment variables}.
///
/// Steps:
/// 1. Checks if it's a dry run; if so, returns {@linkplain TestOutcome.DryPassed a faux result} immediately.
/// 2. Creates the snapshot string based on the {@linkplain SnapshotSources snapshot sources} defined in the test root.
/// 3. Creates the options for ApprovalTests.
///    - This includes the {@link ExampleResultNamer} and {@link SnapshotVerifier#SCRUBBER SCRUBBER}
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
public class SnapshotVerifier {
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
    private final TreeSet<SnapshotSource> snapshotSources;
    private final boolean shareSnapshots;

    public TestOutcome verify(ClientLanguage lang, TestRoot testRoot, FixtureMetadata md, Set<Path> filesForLang, Function<Path, RunResult> result) {
        if (ctx.verifyMode() == DRY_RUN) {
            return TestOutcome.DryPassed.INSTANCE;
        }

        val snapshots = new HashMap<String, Set<Path>>();

        for (val filePath : filesForLang) {
            val runResult = result.apply(filePath);
            val fileSnapshot = mkSnapshot(md, runResult);
            snapshots.computeIfAbsent(fileSnapshot, _ -> new HashSet<>()).add(filePath);
        }

        if (snapshots.size() > 1) {
            return TestOutcome.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.INSTANCE.alsoLog(testRoot, lang, snapshots);
        }

        val snapshot = snapshots.keySet().iterator().next();
        return verifySnapshot(lang, testRoot, snapshot);
    }

    private String mkSnapshot(FixtureMetadata md, RunResult result) {
        val sb = new StringBuilder();

        for (val source : snapshotSources) {
            val snapshot = source.mkSnapshot(ctx, result, md);
            sb.append("---").append(source.name().toLowerCase()).append("---\n");
            sb.append(snapshot).append("\n");
        }

        val snapshot = sb.deleteCharAt(sb.length() - 1).toString().trim();
        return SCRUBBER.scrub(snapshot);
    }

    // TODO maybe cache approved test file contents' hashes + client artifacts so we don't re-verify unchanged tests?
    private TestOutcome verifySnapshot(ClientLanguage lang, TestRoot testRoot, String snapshot) {
        val namer = mkNamer(lang, testRoot);
        val options = mkApprovalOptions(namer);

        try {
            Approvals.verify(snapshot, options);
            return TestOutcome.Passed.INSTANCE;
        } catch (Error e) {
            val expectedPath = Optional.of(namer.getApprovedFile(".txt").toPath())
                .filter(Files::exists);

            return new FailedToVerify(expectedPath).alsoLog(testRoot, lang, snapshot);
        }
    }

    private Options mkApprovalOptions(ExampleResultNamer namer) {
        return new Options()
            .forFile().withNamer(namer)
            .withReporter((_, _) -> true)
            .and(ctx.verifyMode().applyOptions(ctx, namer.getApprovedFile(".txt").toPath()));
    }

    private ExampleResultNamer mkNamer(ClientLanguage language, TestRoot testRoot) {
        return new ExampleResultNamer(ctx, language, testRoot, shareSnapshots);
    }

    private static class $DateScrubber extends RegExScrubber {
        public $DateScrubber() {
            super(Pattern.compile("\\{\\s*\"\\$date\"\\s*:\\s*\\d+\\s*}"), n -> "{ \"$date\" : \"date_" + n + "\" }");
        }
    }
}
