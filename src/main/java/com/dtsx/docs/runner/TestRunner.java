package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.builder.TestPlan;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.TestResults.TestResult;
import com.dtsx.docs.runner.drivers.ClientDriver;
import lombok.Cleanup;
import lombok.val;

public class TestRunner {
    private final VerifierCtx ctx;
    private final ExternalProgram tsx;
    private final TestPlan plan;
    private final ClientDriver driver;
    private final TestVerifier verifier;

    private TestRunner(VerifierCtx ctx, TestPlan plan) {
        this.ctx = ctx;
        this.tsx = ExternalPrograms.tsx(ctx);
        this.plan = plan;
        this.driver = ctx.driver();
        this.verifier = new TestVerifier(ctx);
    }

    public static void runTests(VerifierCtx ctx, TestPlan plan) {
        new TestRunner(ctx, plan).runTests();
    }

    private void runTests() {
        JSFixture.installDependencies(ctx);
        @Cleanup val execEnv = ExecutionEnvironment.setup(ctx, driver);

        val results = new TestResults();

        ctx.reporter().printHeader(plan);

        plan.forEachBaseFixture((baseFixture, mds) -> {
            ctx.reporter().printBaseFixtureHeading(baseFixture, results);

            baseFixture.useResetting(tsx, mds, (md) -> {
                val res = runTest(md, execEnv);
                results.add(baseFixture, res);
                ctx.reporter().printTestResult(baseFixture, res, results);
            });
        });

        ctx.reporter().printSummary(results);
    }

    private TestResult runTest(TestMetadata md, ExecutionEnvironment execEnv) {
        return md.testFixture().use(tsx, () -> {
            return execEnv.withTestFileCopied(md.exampleFile(), () -> {
                return CliLogger.loading("Running %s test".formatted(md.exampleFolder().getFileName()), (_) -> {
                    val result = driver.execute(ctx, execEnv);
                    return verifier.verify(md, result);
                });
            });
        });
    }
}
