package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestPlan;
import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.FixtureMetadata;
import com.dtsx.docs.builder.fixtures.JSFixture.FixtureResetter;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.runner.TestResults.PathsAndOutcome;
import com.dtsx.docs.runner.TestResults.TestOutcome;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.verifier.TestVerifier;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestRunner {
    private final VerifierCtx ctx;
    private final ExternalProgram tsx;
    private final TestPlan plan;
    private final Map<ClientLanguage, ClientDriver> drivers;
    private final TestVerifier verifier;

    private TestRunner(VerifierCtx ctx, TestPlan plan) {
        this.ctx = ctx;
        this.tsx = ExternalPrograms.tsx(ctx);
        this.plan = plan;
        this.drivers = ctx.drivers();
        this.verifier = new TestVerifier(ctx);
    }

    public static boolean runTests(VerifierCtx ctx, TestPlan plan) {
        return new TestRunner(ctx, plan).runAllTests();
    }

    // Don't love using exceptions for control flow, but eh, keeps it simple here
    private static class BailException extends RuntimeException {}

    private boolean runAllTests() {
        val execEnvs = ExecutionEnvironment.setup(ctx, drivers.values());

        val history = new TestResults();

        ctx.reporter().printHeader(plan);

        try {
            plan.forEachBaseFixture((baseFixture, testRoots) -> {
                val md = baseFixture.meta(tsx, execEnvs.nodePath());
                val envVars = PlaceholderResolver.mkEnvVars(ctx, md);

                ctx.reporter().printBaseFixtureHeading(baseFixture, history);

                baseFixture.useResetting(tsx, execEnvs.nodePath(), md, testRoots, (testRoot, _) -> {
                    val result = new TestRootRunner(testRoot, execEnvs, md, envVars).runTestsInRoot();

                    ctx.reporter().printTestRootResults(baseFixture, result, history);
                    history.add(baseFixture, result);

                    if (ctx.bail() && !result.allPassed()) {
                        throw new BailException();
                    }
                });
            });

            return history.allPassed();
        } catch (BailException e) {
            return false;
        } finally {
            ctx.reporter().printSummary(plan, history);
        }
    }

    @RequiredArgsConstructor
    private class TestRootRunner {
        private final TestRoot testRoot;
        private final ExecutionEnvironments execEnvs;
        private final FixtureMetadata md;
        private final Map<String, String> envVars;

        public TestRootResults runTestsInRoot() {
            val outcomes = new HashMap<ClientLanguage, PathsAndOutcome>();

            val testFiles = testRoot.filesToTest().entrySet();

            testRoot.testFixture().useResetting(tsx, execEnvs.nodePath(), md, testFiles, (e, resetter) -> {
                val language = e.getKey();
                val filesForLang = e.getValue();

                val result = runTestsForLanguage(resetter, drivers.get(language), filesForLang, execEnvs.get(language));
                outcomes.put(language, new PathsAndOutcome(filesForLang, result));
            });

            return new TestRootResults(testRoot, outcomes);
        }

        private TestOutcome runTestsForLanguage(FixtureResetter resetter, ClientDriver driver, Set<Path> filesForLang, ExecutionEnvironment execEnv) {
            return verifier.verify(driver.language(), testRoot, md, filesForLang, (path) -> {
                resetter.reset();

                return execEnv.withTestFileCopied(driver, path, md, () -> {
                    val displayPath = testRoot.rootName() + "/" + testRoot.relativeExampleFilePath(path);

                    return CliLogger.loading("Verifying @!%s!@".formatted(displayPath), (_) -> {
                        return driver.execute(ctx, execEnv, envVars);
                    });
                });
            });
        }
    }
}
