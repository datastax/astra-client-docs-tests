package com.dtsx.docs.builder.meta.impls;

import com.dtsx.docs.builder.fixtures.JSFixture;

public sealed interface BaseMetaYml permits CompilesTestMetaYml, SnapshotTestMetaYml {
    JSFixture baseFixture();
}
