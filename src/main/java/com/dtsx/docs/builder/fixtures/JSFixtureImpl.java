package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.nio.file.Path;

@RequiredArgsConstructor
public final class JSFixtureImpl implements JSFixture {
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
        val res = tsx.run("-e", "import * as m from '" + path.toAbsolutePath() + "'; m." + function + "?.()");

        if (res.exitCode() != 0) {
            throw new RuntimeException("Failed to call " + function + " in " + path + ":\nSTDOUT:\n" + res.stdout() + "\nSTDERR:\n" + res.stderr());
        }
    }
}
