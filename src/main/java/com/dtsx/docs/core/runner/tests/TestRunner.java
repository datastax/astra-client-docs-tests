package com.dtsx.docs.core.runner.tests;

import com.dtsx.docs.core.planner.TestPlan;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.runner.ExecutionEnvironment;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.results.TestResults;
import lombok.val;

import java.util.HashMap;
import java.util.Map;

public class TestRunner {
    private final TestCtx ctx;
    private final ExternalProgram tsx;
    private final TestPlan plan;
    private final Map<ClientLanguage, ClientDriver> drivers;

    private TestRunner(TestCtx ctx, TestPlan plan) {
        this.ctx = ctx;
        this.tsx = ExternalPrograms.tsx(ctx);
        this.plan = plan;

        this.drivers = new HashMap<>() {{
            ctx.drivers().forEach((lang, driver) -> {
                if (plan.usedLanguages().contains(lang)) { // filters out unused languages
                    put(lang, driver);
                }
            });
        }};
    }

    public static boolean runTests(TestCtx ctx, TestPlan plan) {
        return new TestRunner(ctx, plan).runAllTests();
    }

    // Don't love using exceptions for control flow, but eh, keeps it simple here
    private static class BailException extends RuntimeException {}

    private boolean runAllTests() {
        val execEnvs = ExecutionEnvironment.setup(ctx, drivers.values(), () -> {
            JSFixture.installDependencies(ctx);
        });

        val history = new TestResults();

        ctx.reporter().printHeader(plan);

        try {
            plan.forEachBaseFixture((baseFixture, testRoots) -> {
                val placeholders = baseFixture.meta(tsx);

                ctx.reporter().printBaseFixtureHeading(baseFixture, history);

                baseFixture.useResetting(tsx, placeholders, testRoots, (testRoot, _) -> {
                    val result = testRoot.testStrategy().runTestsInRoot(tsx, testRoot, execEnvs, placeholders);

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
