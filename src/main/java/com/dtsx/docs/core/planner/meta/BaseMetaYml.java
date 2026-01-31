package com.dtsx.docs.core.planner.meta;

import com.dtsx.docs.core.planner.fixtures.JSFixture;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Optional;

@JsonDeserialize()
public interface BaseMetaYml {
    JSFixture baseFixture();

    interface BaseMetaYmlRep {
        String $schema();

        TestBlock test();

        TestType expectTestType();

        record TestBlock(
            TestType type,
            Optional<Boolean> skip
        ) {}

        enum TestType {
            SNAPSHOT,
            COMPILES
        }
    }
}
