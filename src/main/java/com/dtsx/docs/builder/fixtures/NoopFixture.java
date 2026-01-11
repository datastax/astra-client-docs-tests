package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoopFixture extends JSFixture {
    public static final NoopFixture INSTANCE = new NoopFixture();

    @Override
    public String fixtureName() {
        return "<noop>";
    }

    @Override
    public FixtureMetadata meta(ExternalProgram tsx, Path nodePath) {
        return FixtureMetadata.EMPTY;
    }

    @Override
    protected void setup(ExternalProgram tsx, Path nodePath, FixtureMetadata md) {
        // noop
    }

    @Override
    protected void reset(ExternalProgram tsx, Path nodePath, FixtureMetadata md) {
        // noop
    }

    @Override
    protected void teardown(ExternalProgram tsx, Path nodePath, FixtureMetadata md) {
        // noop
    }
}
