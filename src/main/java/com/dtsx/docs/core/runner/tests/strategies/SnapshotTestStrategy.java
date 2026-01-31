package com.dtsx.docs.core.runner.tests.strategies;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotsShareConfig;
import com.dtsx.docs.core.runner.ExecutionEnvironment.TestFileModifiers;
import com.dtsx.docs.core.runner.tests.snapshots.sources.output.OutputJsonifySource;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotTestMeta;
import com.dtsx.docs.core.runner.ExecutionEnvironment;
import com.dtsx.docs.core.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.core.runner.PlaceholderResolver;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.results.TestOutcome;
import com.dtsx.docs.core.runner.tests.results.TestRootResults;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.core.runner.tests.snapshots.verifier.SnapshotVerifier;
import lombok.Getter;
import lombok.val;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.util.stream.Collectors.toMap;

@Getter
public final class SnapshotTestStrategy extends TestStrategy {
    ///  The test-specific fixture to use for this test root.
    private final JSFixture testFixture;

    /// The set of snapshot sources configured for this test root.
    private final TreeSet<SnapshotSource> snapshotSources;

    /// Whether snapshots are shared across all client languages within this test root.
    ///
    /// If true, a snapshot created by one client language can be used to verify the output of another
    ///  client language within the same test root for stronger consistency and reduced effort.
    private final SnapshotsShareConfig shareConfig;

    public SnapshotTestStrategy(TestCtx ctx, SnapshotTestMeta meta) {
        super(ctx);
        this.testFixture = meta.testFixture();
        this.snapshotSources = meta.snapshotSources();
        this.shareConfig = meta.shareConfig();
    }

    @Override
    public TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, Placeholders placeholders) {
        return new Runner(tsx, testRoot, execEnvs, placeholders).runTestsInRoot();
    }

    private class Runner {
        private final ExternalProgram tsx;
        private final TestRoot testRoot;
        private final ExecutionEnvironments execEnvs;
        private final Placeholders placeholders;
        private final Map<String, String> envVars;
        private final SnapshotVerifier verifier;

        public Runner(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, Placeholders placeholders) {
            this.tsx = tsx;
            this.testRoot = testRoot;
            this.execEnvs = execEnvs;
            this.placeholders = placeholders;
            this.envVars = PlaceholderResolver.mkEnvVars(ctx, placeholders);
            this.verifier = new SnapshotVerifier(ctx, snapshotSources, shareConfig);
        }

        public TestRootResults runTestsInRoot() {
            val outcomes = new HashMap<ClientLanguage, Map<Path, TestOutcome>>();

            val testFiles = testRoot.filesToTest().entrySet();

            testFixture.useResetting(tsx, placeholders, testFiles, (e, resetter) -> {
                val language = e.getKey();
                val filesForLang = e.getValue();

                val result = runTestsForLanguage(resetter, ctx.drivers().get(language), filesForLang, execEnvs.forLanguage(language));

                val outcomesProduct = filesForLang.stream().collect(toMap(path -> path, _ -> result));
                outcomes.put(language, outcomesProduct);
            });

            return new TestRootResults(testRoot, outcomes);
        }

        private TestOutcome runTestsForLanguage(Runnable resetter, ClientDriver driver, Set<Path> filesForLang, ExecutionEnvironment execEnv) {
            return verifier.verify(driver, testRoot, placeholders, filesForLang, (path) -> {
                resetter.run();

                val testFileModifiers = (snapshotSources.stream().anyMatch(OutputJsonifySource.class::isInstance))
                    ? TestFileModifiers.JSONIFY_OUTPUT
                    : TestFileModifiers.NONE;

                return execEnv.withTestFileCopied(driver, path, placeholders, testFileModifiers, () -> {
                    val displayPath = testRoot.displayPath(path);

                    return CliLogger.loading("Verifying @!%s!@".formatted(displayPath), (_) -> {
                        return driver.executeScript(ctx, execEnv, envVars);
                    });
                });
            });
        }
    }
}
