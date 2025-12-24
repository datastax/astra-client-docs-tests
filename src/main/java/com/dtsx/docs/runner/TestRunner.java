package com.dtsx.docs.runner;

import com.dtsx.docs.VerifierConfig;
import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.builder.TestPlan;
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
    private final VerifierConfig cfg;
    private final ExternalProgram tsx;
    private final TestPlan plan;
    private final ClientDriver driver;

    private TestRunner(VerifierConfig cfg, TestPlan plan) {
        this.cfg = cfg;
        this.tsx = ExternalPrograms.tsx(cfg);
        this.plan = plan;
        this.driver = cfg.driver();
    }

    public static void runTests(VerifierConfig cfg, TestPlan plan) {
        new TestRunner(cfg, plan).runTests();
    }

    private void runTests() {
        @Cleanup val execEnv = ExecutionEnvironment.setup(cfg, driver);
        driver.setup(cfg, execEnv);

        val results = new TestResults();

        cfg.reporter().printHeader(plan);

        plan.forEachBaseFixture((baseFixture, mds) -> {
            cfg.reporter().printBaseFixtureHeading(baseFixture, results);

            baseFixture.useResetting(tsx, mds, (md) -> {
                val res = runTest(md, execEnv);
                results.add(baseFixture, res);
                cfg.reporter().printTestResult(baseFixture, res, results);
            });
        });

        cfg.reporter().printSummary(results);
    }

    private TestResult runTest(TestMetadata md, ExecutionEnvironment execEnv) {
        return md.testFixture().use(tsx, () -> {
            return execEnv.useTestFile(md.exampleFile(), (path) -> {
                val result = driver.execute(cfg, path);
                val snapshot = mkSnapshot(md, result);
                return verifySnapshot(md, snapshot);
            });
        });
    }

    private String mkSnapshot(TestMetadata md, RunResult result) {
        return md.snapshotters().stream()
            .map((snapper) -> Pair.create(snapper, snapper.mkSnapshot(cfg, result)))
            .reduce(
                "",
                (acc, pair) -> acc + "---" + pair.getLeft().name().toLowerCase() + "---\n" + pair.getRight() + "\n",
                (a, b) -> a + b
            );
    }

    private TestResults.TestResult verifySnapshot(TestMetadata md, String snapshot) {
        val namer = new ExampleResultNamer(cfg, md);

        try {
            Approvals.verify(snapshot, mkApprovalOptions(md));
            return TestResult.passed(md, namer.getExampleName());
        } catch (Error e) {
            return TestResult.failed(md, namer.getExampleName(), e);
        }
    }

    private Options mkApprovalOptions(TestMetadata md) {
        return new Options()
            .forFile().withNamer(new ExampleResultNamer(cfg, md))
            .withReporter((_, _) -> true);
    }
}
