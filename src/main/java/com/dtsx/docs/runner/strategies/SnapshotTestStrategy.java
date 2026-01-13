package com.dtsx.docs.runner.strategies;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.FixtureMetadata;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.builder.fixtures.JSFixture.FixtureResetter;
import com.dtsx.docs.builder.meta.impls.SnapshotTestMetaYml;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.ExecutionEnvironment;
import com.dtsx.docs.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.runner.PlaceholderResolver;
import com.dtsx.docs.runner.TestResults.TestOutcome;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.snapshots.sources.SnapshotSource;
import com.dtsx.docs.runner.snapshots.verifier.TestVerifier;
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
    private final boolean shareSnapshots;

    public SnapshotTestStrategy(VerifierCtx ctx, SnapshotTestMetaYml meta) {
        super(ctx);
        this.testFixture = meta.testFixture();
        this.snapshotSources = meta.snapshotSources();
        this.shareSnapshots = meta.shareSnapshots();
    }

    @Override
    public TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, FixtureMetadata md) {
        return new Runner(tsx, testRoot, execEnvs, md).runTestsInRoot();
    }

    private class Runner {
        private final ExternalProgram tsx;
        private final TestRoot testRoot;
        private final ExecutionEnvironments execEnvs;
        private final FixtureMetadata md;
        private final Map<String, String> envVars;
        private final TestVerifier verifier;

        public Runner(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, FixtureMetadata md) {
            this.tsx = tsx;
            this.testRoot = testRoot;
            this.execEnvs = execEnvs;
            this.md = md;
            this.envVars = PlaceholderResolver.mkEnvVars(ctx, md);
            this.verifier = new TestVerifier(ctx, snapshotSources, shareSnapshots);
        }

        public TestRootResults runTestsInRoot() {
            val outcomes = new HashMap<ClientLanguage, Map<Path, TestOutcome>>();

            val testFiles = testRoot.filesToTest().entrySet();

            testFixture.useResetting(tsx, md, testFiles, (e, resetter) -> {
                val language = e.getKey();
                val filesForLang = e.getValue();

                val result = runTestsForLanguage(resetter, ctx.drivers().get(language), filesForLang, execEnvs.forLanguage(language));

                val outcomesProduct = filesForLang.stream().collect(toMap(path -> path, _ -> result));
                outcomes.put(language, outcomesProduct);
            });

            return new TestRootResults(testRoot, outcomes);
        }

        private TestOutcome runTestsForLanguage(FixtureResetter resetter, ClientDriver driver, Set<Path> filesForLang, ExecutionEnvironment execEnv) {
            return verifier.verify(driver.language(), testRoot, md, filesForLang, (path) -> {
                resetter.reset();

                return execEnv.withTestFileCopied(driver, path, md, () -> {
                    val displayPath = testRoot.displayPath(path);

                    return CliLogger.loading("Verifying @!%s!@".formatted(displayPath), (_) -> {
                        return driver.executeScript(ctx, execEnv, envVars);
                    });
                });
            });
        }
    }
}
