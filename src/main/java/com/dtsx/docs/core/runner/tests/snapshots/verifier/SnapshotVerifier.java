package com.dtsx.docs.core.runner.tests.snapshots.verifier;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.results.TestOutcome;
import com.dtsx.docs.core.runner.tests.results.TestOutcome.FailedToVerify;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
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

import static com.dtsx.docs.core.runner.tests.VerifyMode.DRY_RUN;

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

    private final TestCtx ctx;
    private final TreeSet<SnapshotSource> snapshotSources;
    private final boolean shareSnapshots;

    public TestOutcome verify(ClientDriver driver, TestRoot testRoot, Placeholders placeholders, Set<Path> filesForLang, Function<Path, RunResult> result) {
        if (ctx.verifyMode() == DRY_RUN) {
            return TestOutcome.DryPassed.INSTANCE;
        }

        val snapshots = new HashMap<String, Set<Path>>();

        for (val filePath : filesForLang) {
            val runResult = result.apply(filePath);
            val fileSnapshot = mkSnapshot(driver, placeholders, runResult);
            snapshots.computeIfAbsent(fileSnapshot, _ -> new HashSet<>()).add(filePath);
        }

        if (snapshots.size() > 1) {
            return TestOutcome.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.Mismatch.INSTANCE.alsoLog(testRoot, driver.language(), snapshots);
        }

        val snapshot = snapshots.keySet().iterator().next();
        return verifySnapshot(driver, testRoot, snapshot);
    }

    private String mkSnapshot(ClientDriver driver, Placeholders placeholders, RunResult result) {
        val sb = new StringBuilder();

        for (val source : snapshotSources) {
            val snapshot = source.mkSnapshot(ctx, driver, result, placeholders);
            sb.append("---").append(source.name().toLowerCase()).append("---\n");
            sb.append(snapshot).append("\n");
        }

        val snapshot = sb.deleteCharAt(sb.length() - 1).toString().trim();
        return SCRUBBER.scrub(snapshot);
    }

    // TODO maybe cache approved test file contents' hashes + client artifacts so we don't re-verify unchanged tests?
    private TestOutcome verifySnapshot(ClientDriver driver, TestRoot testRoot, String snapshot) {
        val namer = mkNamer(driver.language(), testRoot);
        val options = mkApprovalOptions(namer);

        try {
            Approvals.verify(snapshot, options);
            return TestOutcome.Passed.INSTANCE;
        } catch (Error e) {
            val expectedPath = Optional.of(namer.getApprovedFile(".txt").toPath())
                .filter(Files::exists);

            return new FailedToVerify(expectedPath).alsoLog(testRoot, driver.language(), snapshot);
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
