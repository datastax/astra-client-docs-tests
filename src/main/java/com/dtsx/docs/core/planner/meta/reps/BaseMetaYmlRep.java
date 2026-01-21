package com.dtsx.docs.core.planner.meta.reps;

import java.util.Optional;

public sealed interface BaseMetaYmlRep permits CompilesTestMetaYmlRep, SnapshotTestMetaYmlRep {
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
