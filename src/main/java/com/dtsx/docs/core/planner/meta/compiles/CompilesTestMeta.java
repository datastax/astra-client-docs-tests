package com.dtsx.docs.core.planner.meta.compiles;

import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.planner.fixtures.NoopFixture;
import com.dtsx.docs.core.planner.meta.BaseMetaYml;
import com.dtsx.docs.core.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestBlock.SkipConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public final class CompilesTestMeta implements BaseMetaYml {
    private final SkipConfig skipConfig;

    public CompilesTestMeta(TestCtx ctx, BaseMetaYmlRep meta) {
        this.skipConfig = SkipConfig.parse(SkipConfig::new, ctx, meta.test().skipConfig());
    }

    @Override
    public JSFixture baseFixture() {
        return NoopFixture.COMPILATION_TESTS_INSTANCE;
    }
}
