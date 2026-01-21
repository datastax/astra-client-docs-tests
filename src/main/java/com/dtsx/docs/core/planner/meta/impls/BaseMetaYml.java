package com.dtsx.docs.core.planner.meta.impls;

import com.dtsx.docs.core.planner.fixtures.JSFixture;

public sealed interface BaseMetaYml permits CompilesTestMetaYml, SnapshotTestMetaYml {
    JSFixture baseFixture();
}
