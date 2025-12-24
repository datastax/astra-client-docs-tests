package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;

public enum NoopFixture implements JSFixture {
    INSTANCE;

    @Override
    public String fixtureName() {
        return "None";
    }

    @Override
    public void setup(ExternalProgram tsx) {
        // noop
    }

    @Override
    public void reset(ExternalProgram tsx) {
        // noop
    }

    @Override
    public void teardown(ExternalProgram tsx) {
        // noop
    }
}
