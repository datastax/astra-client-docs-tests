package com.dtsx.docs.builder.meta.impls;

import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.builder.fixtures.NoopFixture;

public final class CompilesTestMetaYml implements BaseMetaYml {
    @Override
    public JSFixture baseFixture() {
        return NoopFixture.COMPILATION_TESTS_INSTANCE;
    }
}
