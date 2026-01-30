package com.dtsx.docs.core.planner.meta.compiles;

import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.planner.fixtures.NoopFixture;
import com.dtsx.docs.core.planner.meta.BaseMetaYml;

public final class CompilesTestMetaYml implements BaseMetaYml {
    @Override
    public JSFixture baseFixture() {
        return NoopFixture.COMPILATION_TESTS_INSTANCE;
    }
}
