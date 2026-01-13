package com.dtsx.docs.runner.strategies;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.FixtureMetadata;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.runner.PlaceholderResolver;
import com.dtsx.docs.runner.TestResults.TestOutcome;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.val;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class CompilesTestStrategy extends TestStrategy {
    public CompilesTestStrategy(VerifierCtx ctx) {
        super(ctx);
    }

    @Override
    public TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, FixtureMetadata md) {
        val outcomes = new HashMap<ClientLanguage, Map<Path, TestOutcome>>();

        testRoot.filesToTest().forEach((lang, paths) -> {
            val driver = ctx.drivers().get(lang);
            val execEnv = execEnvs.forLanguage(lang);

            // TODO: should this be affected by dry running? (i.e. return early w/ DryPassed)
            for (val path : paths) {
                execEnv.withTestFileCopied(driver, path, md, () -> {
                    val displayPath = testRoot.displayPath(path);

                    val res = CliLogger.loading("Compiling @!%s!@".formatted(displayPath), (_) -> {
                        return driver.compileScript(ctx, execEnv, mkEnv());
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

    private Map<String, String> mkEnv() {
        return PlaceholderResolver.mkEnvVars(ctx, new FixtureMetadata(
            "compiles_test_collection",
            "compiles_test_table",
            "compiles_test_keyspace"
        ));
    }
}
