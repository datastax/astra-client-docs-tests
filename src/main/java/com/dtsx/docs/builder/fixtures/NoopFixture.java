package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
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
    public FixtureMetadata meta(ExternalProgram tsx) {
        return FixtureMetadata.EMPTY;
    }

    @Override
    protected void setup(ExternalProgram tsx, FixtureMetadata md) {
        // noop
    }

    @Override
    protected void reset(ExternalProgram tsx, FixtureMetadata md) {
        // noop
    }

    @Override
    protected void teardown(ExternalProgram tsx, FixtureMetadata md) {
        // noop
    }
}
