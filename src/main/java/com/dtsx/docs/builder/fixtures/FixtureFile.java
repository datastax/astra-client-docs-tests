package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;

import java.nio.file.Path;

@RequiredArgsConstructor
public sealed abstract class FixtureFile permits BaseFixture, TestFixture {
    @Getter
    @ToString.Include
    private final Path path;

    protected static final String SETUP_FUNCTION_NAME = "Setup";
    protected static final String RESET_FUNCTION_NAME = "Reset";
    protected static final String TEARDOWN_FUNCTION_NAME = "Teardown";

    protected void tryCallJsFunction(ExternalProgram tsx, String function) {
        val res = tsx.run("-e", "import * as m from '" + path.toAbsolutePath() + "'; m." + function + "?.()");

        if (res.exitCode() != 0) {
            throw new RuntimeException("Failed to call " + function + " in " + path + ":\nSTDOUT:\n" + res.stdout() + "\nSTDERR:\n" + res.stderr());
        }
    }
}
