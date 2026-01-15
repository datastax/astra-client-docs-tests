package com.dtsx.docs.planner.meta.impls;

import com.dtsx.docs.planner.fixtures.JSFixture;

public sealed interface BaseMetaYml permits CompilesTestMetaYml, SnapshotTestMetaYml {
    JSFixture baseFixture();
}
