package com.dtsx.docs.runner.tests.strategies;

import com.dtsx.docs.planner.TestRoot;
import com.dtsx.docs.runner.Placeholders;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.runner.tests.results.TestOutcome;
import com.dtsx.docs.runner.tests.results.TestRootResults;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.val;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.dtsx.docs.runner.tests.VerifyMode.DRY_RUN;

public final class CompilesTestStrategy extends TestStrategy {
    private static final Placeholders FAKE_FIXTURE_MD = new Placeholders(
        "compiles_test_collection",
        "compiles_test_table",
        "compiles_test_keyspace"
    );

    public CompilesTestStrategy(TestCtx ctx) {
        super(ctx);
    }

    @Override
    public TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, Placeholders ignored) {
        val outcomes = new HashMap<ClientLanguage, Map<Path, TestOutcome>>();

        testRoot.filesToTest().forEach((lang, paths) -> {
            val driver = ctx.drivers().get(lang);
            val execEnv = execEnvs.forLanguage(lang);

            for (val path : paths) {
                if (ctx.verifyMode() == DRY_RUN) {
                    outcomes.computeIfAbsent(lang, _ -> new HashMap<>()).put(path, TestOutcome.DryPassed.INSTANCE);
                    continue;
                }

                execEnv.withTestFileCopied(driver, path, FAKE_FIXTURE_MD, () -> {
                    val displayPath = testRoot.displayPath(path);

                    val res = CliLogger.loading("Compiling @!%s!@".formatted(displayPath), (_) -> {
                        return driver.compileScript(ctx, execEnv);
                    });

                    val outcome = (res.notOk())
                        ? new TestOutcome.FailedToCompile(res.output()).alsoLog(testRoot, lang, res.output())
                        : TestOutcome.Passed.INSTANCE;

                    outcomes.computeIfAbsent(lang, _ -> new HashMap<>()).put(path, outcome);
                    return null;
                });
            }
        });

        return new TestRootResults(testRoot, outcomes);
    }
}
