package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@EqualsAndHashCode
@RequiredArgsConstructor
public final class NoopFixture implements JSFixture {
    public static final NoopFixture INSTANCE = new NoopFixture("<noop>");

    private final String name;

    @Override
    public String fixtureName() {
        return name;
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
