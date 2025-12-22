package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;

import java.nio.file.Path;

public final class BaseFixture extends FixtureFile {
    public BaseFixture(Path path) {
        super(path);
    }

    public void setup(ExternalProgram tsx) {
        tryCallJsFunction(tsx, SETUP_FUNCTION_NAME);
    }

    public void reset(ExternalProgram tsx) {
        tryCallJsFunction(tsx, RESET_FUNCTION_NAME);
    }

    public void teardown(ExternalProgram tsx) {
        tryCallJsFunction(tsx, TEARDOWN_FUNCTION_NAME);
    }
}
