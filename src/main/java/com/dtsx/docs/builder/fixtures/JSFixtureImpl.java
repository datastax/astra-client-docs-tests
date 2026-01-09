package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.TestRunException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RequiredArgsConstructor
public final class JSFixtureImpl extends JSFixture {
    private final VerifierCtx ctx;
    private final Path path;
    private final boolean dryRun;

    private static final String VERIFY_CAN_RUN_NAME = "VerifyJSFixtureCanRun"; // will never exist but will still force the module to be loaded
    private static final String SETUP_FUNCTION_NAME = "Setup";
    private static final String RESET_FUNCTION_NAME = "Reset";
    private static final String TEARDOWN_FUNCTION_NAME = "Teardown";

    @Override
    public String fixtureName() {
        return path.getFileName().toString();
    }

    @Override
    public void setup(ExternalProgram tsx, Path nodePath) {
        tryCallJsFunction(tsx, nodePath, VERIFY_CAN_RUN_NAME);

        if (!dryRun) {
            tryCallJsFunction(tsx, nodePath, SETUP_FUNCTION_NAME);
        }
    }

    @Override
    public void reset(ExternalProgram tsx, Path nodePath) {
        if (!dryRun) {
            tryCallJsFunction(tsx, nodePath, RESET_FUNCTION_NAME);
        }
    }

    @Override
    public void teardown(ExternalProgram tsx, Path nodePath) {
        if (!dryRun) {
            tryCallJsFunction(tsx, nodePath, TEARDOWN_FUNCTION_NAME);
        }
    }

    @SneakyThrows
    private void tryCallJsFunction(ExternalProgram tsx, Path nodePath, String function) {
        val displayPath = ctx.examplesFolder().relativize(path);

        if (!nodePath.endsWith("node_modules") || !Files.exists(nodePath)) {
            throw new TestRunException("Invalid dependencies directory '" + nodePath + "'"); // should never be thrown, but just in case
        }

        val envVars = Map.of("NODE_PATH", nodePath.toString());

        // Calls the function if it exists
        val res = CliLogger.loading("Calling @!%s!@ in @!%s!@".formatted(function, displayPath), (_) -> {
            return tsx.run(null, envVars, "-e", "import * as m from '" + path.toAbsolutePath() + "'; m." + function + "?.()");
        });

        if (res.exitCode() != 0) {
            throw new TestRunException("Failed to call " + function + " in " + path + ":\nSTDOUT:\n" + res.stdout() + "\nSTDERR:\n" + res.stderr());
        }
    }
}
