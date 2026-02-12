package com.dtsx.docs.core.runner.tests.strategies;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.runner.ExecutionEnvironment;
import com.dtsx.docs.core.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.core.runner.ExecutionEnvironment.TestFileModifiers;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.results.TestOutcome;
import com.dtsx.docs.core.runner.tests.results.TestRootResults;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.val;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.dtsx.docs.core.runner.tests.VerifyMode.DRY_RUN;
import static com.dtsx.docs.core.runner.tests.VerifyMode.NO_COMPILE_ONLY;

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
        val outcomes = new ConcurrentHashMap<ClientLanguage, Map<Path, TestOutcome>>();

        val numFiles = new Object() {
            int ref = 0;
        };

        testRoot.filesToTest().forEach((lang, files) -> {
            outcomes.put(lang, new ConcurrentHashMap<>());
            numFiles.ref += files.size();
        });

        val displayMsg = "Compiling @!%d!@ file%s for @!%s!@".formatted(numFiles.ref, (numFiles.ref == 1) ? "" : "s", testRoot.rootName());

        return CliLogger.loading(displayMsg, (_) -> {
            try (val executor = Executors.newVirtualThreadPerTaskExecutor()) {
                val futures = new ArrayList<Future<?>>();

                testRoot.filesToTest().forEach((lang, paths) -> {
                    val driver = ctx.drivers().get(lang);
                    val execEnv = execEnvs.forLanguage(lang);

                    futures.add(executor.submit(() -> {
                        paths.forEach(path -> {
                            runSingleTest(testRoot, outcomes, lang, path, driver, execEnv);
                        });
                    }));
                });

                for (val future : futures) {
                    future.get();
                }
            }

            return new TestRootResults(testRoot, outcomes);
        });
    }

    private void runSingleTest(TestRoot testRoot, ConcurrentHashMap<ClientLanguage, Map<Path, TestOutcome>> outcomes, ClientLanguage lang, Path path, ClientDriver driver, ExecutionEnvironment execEnv) {
        if (ctx.verifyMode() == DRY_RUN || ctx.verifyMode() == NO_COMPILE_ONLY) {
            outcomes.get(lang).put(path, TestOutcome.DryPassed.INSTANCE);
            return;
        }

        execEnv.withTestFileCopied(driver, path, FAKE_FIXTURE_MD, TestFileModifiers.NONE, () -> {
            val res = driver.compileScript(ctx, execEnv);

            val outcome = (res.notOk())
                ? new TestOutcome.FailedToCompile(res.output()).alsoLog(testRoot, lang, res.output())
                : TestOutcome.Passed.INSTANCE;

            outcomes.get(lang).put(path, outcome);
            return null;
        });
    }
}
