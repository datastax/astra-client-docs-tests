package com.dtsx.docs.planner.meta.impls;

import com.dtsx.docs.planner.fixtures.JSFixture;
import com.dtsx.docs.planner.fixtures.NoopFixture;

public final class CompilesTestMetaYml implements BaseMetaYml {
    @Override
    public JSFixture baseFixture() {
        return NoopFixture.COMPILATION_TESTS_INSTANCE;
    }
}
