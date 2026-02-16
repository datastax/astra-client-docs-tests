package com.dtsx.docs.core.runner.tests.snapshots.verifier;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotsShareConfig;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.results.TestOutcome;
import com.dtsx.docs.core.runner.tests.results.TestOutcome.FailedToVerify;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.approvaltests.core.Scrubber;
import org.approvaltests.scrubbers.GuidScrubber;
import org.approvaltests.scrubbers.MultiScrubber;
import org.approvaltests.scrubbers.RegExScrubber;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.dtsx.docs.core.runner.tests.VerifyMode.DRY_RUN;
import static com.dtsx.docs.core.runner.tests.VerifyMode.NORMAL;

@RequiredArgsConstructor
public class SnapshotVerifier {
    private static final String LAST_MODIFIED_FILE = "last-modified.txt";

    /// Scrubber to clean dynamic data from snapshots before verification, replacing them with incrementing placeholders.
    ///
    /// Currently, includes:
    ///   - {@link GuidScrubber}: Scrubs UUIDs
    ///   - {@link $DateScrubber $DateScrubber}: Scrubs collection `$date`s
    public static final Scrubber SCRUBBER = new MultiScrubber(List.of(
        new GuidScrubber(),
        new $DateScrubber(),
        new ObjectIdScrubber()
    ));

    private final TestCtx ctx;
    private final List<SnapshotSource> snapshotSources;
    private final SnapshotsShareConfig shareConfig;

    @SneakyThrows
    @SuppressWarnings("BusyWait")
    public TestOutcome verify(ClientDriver driver, TestRoot testRoot, Placeholders placeholders, Path filePath, ThrowingSupplier<RunResult> result) {
        if (ctx.verifyMode() == DRY_RUN) {
            return TestOutcome.DryPassed.INSTANCE;
        }

        for (var i = 0; true; i++) {
            try {
                val runResult = result.get();
                val snapshot = mkSnapshot(driver, placeholders, runResult);
                return verifySnapshot(driver, testRoot, snapshot);
            } catch (Exception e) {
                val lowercaseMessage = e.getMessage().toLowerCase();
                if (i < 2 && (lowercaseMessage.contains("timeout") || lowercaseMessage.contains("timed out"))) {
                    CliLogger.exception("Retrying due to timeout when verifying file '" + filePath + "'", e);
                    Thread.sleep(1000);
                    continue;
                }
                throw e;
            }
        }
    }
    
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
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

            // updates last-modified.txt when a new *.received.txt file is created
            // allows the reviewing dashboard to automatically detect changes in the snapshots
            if (ctx.verifyMode() == NORMAL) {
                updateLastModified(ctx.snapshotsFolder());
            }

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
        return new ExampleResultNamer(ctx, language, testRoot, shareConfig);
    }

    private static void updateLastModified(Path snapshotsFolder) {
        try {
            val lastModifiedPath = snapshotsFolder.resolve(LAST_MODIFIED_FILE);
            Files.writeString(lastModifiedPath, Instant.now().toString());
        } catch (IOException e) {
            CliLogger.exception("Failed to update " + LAST_MODIFIED_FILE + " in snapshots folder '" + snapshotsFolder + "'", e);
        }
    }

    public static class $DateScrubber extends RegExScrubber {
        public static final Pattern DATE_PATTERN = Pattern.compile("\\{\\s*\"\\$date\"\\s*:\\s*\\d+\\s*}");

        public $DateScrubber() {
            super(DATE_PATTERN, n -> "{ \"$date\" : \"date_" + n + "\" }");
        }
    }

    public static class ObjectIdScrubber extends RegExScrubber {
        public static final Pattern OBJECT_ID_PATTERN = Pattern.compile("[a-fA-F0-9]{24}");

        public ObjectIdScrubber() {
            super(OBJECT_ID_PATTERN, n -> "objectId_" + n);
        }
    }
}
