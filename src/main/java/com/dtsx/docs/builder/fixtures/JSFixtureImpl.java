package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.nio.file.Path;

@EqualsAndHashCode
@RequiredArgsConstructor
public final class JSFixtureImpl implements JSFixture {
    @EqualsAndHashCode.Exclude
    private final VerifierCtx ctx;
    private final Path path;

    private static final String SETUP_FUNCTION_NAME = "Setup";
    private static final String RESET_FUNCTION_NAME = "Reset";
    private static final String TEARDOWN_FUNCTION_NAME = "Teardown";

    @Override
    public String fixtureName() {
        return path.getFileName().toString();
    }

    @Override
    public void setup(ExternalProgram tsx) {
        tryCallJsFunction(tsx, SETUP_FUNCTION_NAME);
    }

    @Override
    public void reset(ExternalProgram tsx) {
        tryCallJsFunction(tsx, RESET_FUNCTION_NAME);
    }

    @Override
    public void teardown(ExternalProgram tsx) {
        tryCallJsFunction(tsx, TEARDOWN_FUNCTION_NAME);
    }

    private void tryCallJsFunction(ExternalProgram tsx, String function) {
        val displayPath = ctx.examplesFolder().relativize(path);

        val res = CliLogger.loading("Calling @!%s!@ in @!%s!@".formatted(function, displayPath), (_) -> {
            return tsx.run("-e", "import * as m from '" + path.toAbsolutePath() + "'; m." + function + "?.()");
        });

        if (res.exitCode() != 0) {
            throw new RuntimeException("Failed to call " + function + " in " + path + ":\nSTDOUT:\n" + res.stdout() + "\nSTDERR:\n" + res.stderr());
        }
    }
}
