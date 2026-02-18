package com.dtsx.docs.core.runner.tests;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.TestPlan;
import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.runner.ExecutionEnvironment;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.results.TestResults;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
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
            plan.forEachPool((pool, testRoots) -> {
                ctx.reporter().printBaseFixtureHeading(pool.fixture(), history);
                
                try {
                    pool.setup(tsx);

                    for (val testRoot : testRoots) {
                        try {
                            val adaptedPool = testRoot.testStrategy().slicePool(testRoot, pool);

                            val startTime = System.currentTimeMillis();
                            val result = testRoot.testStrategy().runTestsInRoot(tsx, testRoot, execEnvs, adaptedPool);
                            val duration = System.currentTimeMillis() - startTime;

                            ctx.reporter().printTestRootResults(pool.fixture(), result, history, duration);
                            history.add(pool.fixture(), result);
                            
                            if (ctx.bail() && !result.allPassed()) {
                                throw new BailException();
                            }
                        } catch (Exception e) {
                            CliLogger.exception("Error running tests in test root '" + testRoot.rootName() + "' (" + e.getClass().getSimpleName() + ")");
                            throw e;
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    pool.teardown(tsx);
                }
            });

            return history.allPassed();
        } catch (BailException e) {
            return false;
        } finally {
            ctx.reporter().printSummary(plan, history);
        }
    }
}
