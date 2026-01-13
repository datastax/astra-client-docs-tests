package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestPlan;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.drivers.ClientDriver;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.val;

import java.util.Map;

public class TestRunner {
    private final VerifierCtx ctx;
    private final ExternalProgram tsx;
    private final TestPlan plan;
    private final Map<ClientLanguage, ClientDriver> drivers;

    private TestRunner(VerifierCtx ctx, TestPlan plan) {
        this.ctx = ctx;
        this.tsx = ExternalPrograms.tsx(ctx);
        this.plan = plan;
        this.drivers = ctx.drivers();
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
                val md = baseFixture.meta(tsx);

                ctx.reporter().printBaseFixtureHeading(baseFixture, history);

                baseFixture.useResetting(tsx, md, testRoots, (testRoot, _) -> {
                    val result = testRoot.testStrategy().runTestsInRoot(tsx, testRoot, execEnvs, md);

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
}
