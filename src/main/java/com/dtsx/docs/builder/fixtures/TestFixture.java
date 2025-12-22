package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;

import java.nio.file.Path;

public final class TestFixture extends FixtureFile {
    public TestFixture(Path path) {
        super(path);
    }

    public void setup(ExternalProgram tsx) {
        tryCallJsFunction(tsx, SETUP_FUNCTION_NAME);
    }

    public void teardown(ExternalProgram tsx) {
        tryCallJsFunction(tsx, TEARDOWN_FUNCTION_NAME);
    }
}
