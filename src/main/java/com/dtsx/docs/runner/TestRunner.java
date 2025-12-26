package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.builder.TestPlan;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.runner.TestResults.TestResult;
import com.dtsx.docs.runner.drivers.ClientDriver;
import lombok.Cleanup;
import lombok.val;
import org.approvaltests.Approvals;
import org.approvaltests.core.Options;
import org.graalvm.collections.Pair;

public class TestRunner {
    private final VerifierCtx ctx;
    private final ExternalProgram tsx;
    private final TestPlan plan;
    private final ClientDriver driver;

    private TestRunner(VerifierCtx ctx, TestPlan plan) {
        this.ctx = ctx;
        this.tsx = ExternalPrograms.tsx(ctx);
        this.plan = plan;
        this.driver = ctx.driver();
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
            return CliLogger.loading("Running %s test".formatted(md.exampleFolder().getFileName()), (_) -> {
                return execEnv.withTestFileCopied(md.exampleFile(), () -> {
                    val result = driver.execute(ctx, execEnv);
                    val snapshot = mkSnapshot(md, result);
                    return verifySnapshot(md, snapshot);
                });
            });
        });
    }

    private String mkSnapshot(TestMetadata md, RunResult result) {
        return md.snapshotters().stream()
            .map((snapper) -> Pair.create(snapper, snapper.mkSnapshot(ctx, result)))
            .reduce(
                "",
                (acc, pair) -> acc + "---" + pair.getLeft().name().toLowerCase() + "---\n" + pair.getRight() + "\n",
                (a, b) -> a + b
            );
    }

    private TestResult verifySnapshot(TestMetadata md, String snapshot) {
        val namer = new ExampleResultNamer(ctx, md);

        try {
            Approvals.verify(snapshot, mkApprovalOptions(md));
            return TestResult.passed(md, namer.getExampleName());
        } catch (Error e) {
            return TestResult.failed(md, namer.getExampleName(), e);
        }
    }

    private Options mkApprovalOptions(TestMetadata md) {
        return new Options()
            .forFile().withNamer(new ExampleResultNamer(ctx, md))
            .withReporter((_, _) -> true);
    }
}
