package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestPlan;
import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.runner.TestResults.TestOutcome;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import com.dtsx.docs.runner.verifier.TestVerifier;
import lombok.Cleanup;
import lombok.val;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    private boolean runAllTests() {
        JSFixture.installDependencies(ctx);
        @Cleanup val execEnvs = ExecutionEnvironment.setup(ctx, drivers.values());

        val history = new TestResults();

        ctx.reporter().printHeader(plan);

        plan.forEachBaseFixture((baseFixture, testRoots) -> {
            ctx.reporter().printBaseFixtureHeading(baseFixture, history);

            baseFixture.useResetting(tsx, testRoots, (testRoot) -> {
                val result = runTestsInRoot(testRoot, execEnvs);
                ctx.reporter().printTestRootResults(baseFixture, result, history);
                history.add(baseFixture, result);
            });
        });

        ctx.reporter().printSummary(history);

        return history.allApproved();
    }

    private TestRootResults runTestsInRoot(TestRoot testRoot, ExecutionEnvironments execEnvs) {
        val outcomes = new HashMap<ClientLanguage, TestOutcome>();

        testRoot.testFixture().useResetting(tsx, testRoot.filesToTest().entrySet(), (e) -> {
            val language = e.getKey();
            val exampleFile = e.getValue();

            val result = runSpecificTest(drivers.get(language), testRoot, exampleFile, execEnvs.get(language));
            outcomes.put(language, result);
        });

        return new TestRootResults(testRoot, outcomes);
    }

    private TestOutcome runSpecificTest(ClientDriver driver, TestRoot testRoot, Path exampleFile, ExecutionEnvironment execEnv) {
        val displayPath = testRoot.path().getParent().relativize(exampleFile).toString();

        return execEnv.withTestFileCopied(driver, exampleFile, () -> {
            return CliLogger.loading("Testing @!%s!@".formatted(displayPath), (_) -> {
                return verifier.verify(driver.language(), testRoot, () -> driver.execute(ctx, execEnv));
            });
        });
    }
}
