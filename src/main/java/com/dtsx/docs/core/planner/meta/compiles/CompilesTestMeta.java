package com.dtsx.docs.core.planner.meta.compiles;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.planner.fixtures.NoopFixture;
import com.dtsx.docs.core.planner.meta.BaseMetaYml;
import com.dtsx.docs.core.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig;
import lombok.Getter;

@Getter
public final class CompilesTestMeta implements BaseMetaYml {
    private final SkipConfig skipConfig;
    private final boolean parallel;

    public CompilesTestMeta(TestCtx ctx, BaseMetaYmlRep meta) {
        this.skipConfig = SkipConfig.parse(SkipConfig::new, ctx, meta.test().skip());
        this.parallel = meta.test().parallel().orElse(true);
    }

    @Override
    public JSFixture baseFixture() {
        return NoopFixture.COMPILATION_TESTS_INSTANCE;
    }
}
