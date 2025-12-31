package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestMetadata;
import com.dtsx.docs.builder.TestPlan;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.runner.TestResults.TestResult;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.Cleanup;
import lombok.val;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

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

    public static void runTests(VerifierCtx ctx, TestPlan plan) {
        new TestRunner(ctx, plan).runAllTests();
    }

    private void runAllTests() {
        JSFixture.installDependencies(ctx);
        @Cleanup val execEnvs = ExecutionEnvironment.setup(ctx, drivers.values());

        val allResults = new TestResults();

        ctx.reporter().printHeader(plan);

        plan.forEachBaseFixture((baseFixture, mds) -> {
            ctx.reporter().printBaseFixtureHeading(baseFixture, allResults);

            baseFixture.useResetting(tsx, mds, (md) -> {
                runTestsForExample(md, execEnvs, (result) -> {
                    allResults.add(baseFixture, result);
                    ctx.reporter().printTestResult(baseFixture, result, allResults);
                });
            });
        });

        ctx.reporter().printSummary(allResults);
    }

    private void runTestsForExample(TestMetadata md, ExecutionEnvironments execEnvs, Consumer<TestResult> resultConsumer) {
        md.testFixture().useResetting(tsx, md.exampleFiles(), (pair) -> {
            val language = pair.getLeft();
            val exampleFile = pair.getRight();

            val result = runSpecificTest(drivers.get(language), md, exampleFile, execEnvs.get(language));
            resultConsumer.accept(result);
        });
    }

    private TestResult runSpecificTest(ClientDriver driver, TestMetadata md, Path exampleFile, ExecutionEnvironment execEnv) {
        val displayPath = md.exampleFolder().getParent().relativize(exampleFile).toString();

        return execEnv.withTestFileCopied(driver, exampleFile, () -> {
            return CliLogger.loading("Testing @!%s!@".formatted(displayPath), (_) -> {
                val result = driver.execute(ctx, execEnv);
                return verifier.verify(driver.language(), md, exampleFile, result);
            });
        });
    }
}
