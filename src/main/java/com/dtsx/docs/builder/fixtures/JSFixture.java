package com.dtsx.docs.builder.fixtures;

import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.val;

import java.util.function.Consumer;
import java.util.function.Supplier;

public sealed interface JSFixture permits NoopFixture, JSFixtureImpl {
    String fixtureName();

    void setup(ExternalProgram tsx);
    void reset(ExternalProgram tsx);
    void teardown(ExternalProgram tsx);

    default <T> T use(ExternalProgram tsx, Supplier<T> supplier) {
        setup(tsx);
        try {
            return supplier.get();
        } finally {
            teardown(tsx);
        }
    }

    default <T> void useResetting(ExternalProgram tsx, Iterable<T> t, Consumer<T> consumer) {
        setup(tsx);
        try {
            for (val item : t) {
                consumer.accept(item);
                reset(tsx);
            }
        } finally {
            teardown(tsx);
        }
    }
}
