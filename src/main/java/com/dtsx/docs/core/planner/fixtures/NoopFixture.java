package com.dtsx.docs.core.planner.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.core.runner.Placeholders;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoopFixture extends JSFixture {
    public static final NoopFixture SNAPSHOT_TESTS_INSTANCE = new NoopFixture("no base fixture");
    public static final NoopFixture COMPILATION_TESTS_INSTANCE = new NoopFixture("compilation tests");

    private final String name;

    @Override
    public String fixtureName() {
        return name;
    }

    @Override
    public Placeholders meta(ExternalProgram tsx) {
        return Placeholders.EMPTY;
    }

    @Override
    protected void setup(ExternalProgram tsx, Placeholders placeholders) {
        // noop
    }

    @Override
    protected void reset(ExternalProgram tsx, Placeholders placeholders) {
        // noop
    }

    @Override
    protected void teardown(ExternalProgram tsx, Placeholders placeholders) {
        // noop
    }
}
