package com.dtsx.docs.core.planner.meta;

import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.planner.meta.compiles.CompilesTestMetaYml;
import com.dtsx.docs.core.planner.meta.compiles.CompilesTestMetaYmlRep;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotTestMetaYml;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotTestMetaYmlRep;
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
