package com.dtsx.docs.core.planner.meta.snapshot;

import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.planner.fixtures.BaseFixturePool;
import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.runner.tests.strategies.execution.ExecutionStrategy;
import com.dtsx.docs.core.runner.tests.strategies.execution.IsolatedExecutionStrategy;
import com.dtsx.docs.core.runner.tests.strategies.execution.SequentialExecutionStrategy;
import com.dtsx.docs.core.runner.tests.strategies.execution.SharedExecutionStrategy;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.RequiredArgsConstructor;
import org.lambda.functions.Function4;

import java.util.function.BiFunction;

@RequiredArgsConstructor
public enum ExecutionMode {
    SEQUENTIAL(SequentialExecutionStrategy::new, SequentialExecutionStrategy::slicePool),
    SHARED(SharedExecutionStrategy::new, SharedExecutionStrategy::slicePool),
    ISOLATED(IsolatedExecutionStrategy::new, IsolatedExecutionStrategy::slicePool);

    private final Function4<ExternalProgram, JSFixture, BaseFixturePool, TestRoot, ExecutionStrategy> constructor;
    private final BiFunction<BaseFixturePool, TestRoot, BaseFixturePool> slicePool;

    public BaseFixturePool slicePool(BaseFixturePool pool, TestRoot testRoot) {
        return slicePool.apply(pool, testRoot);
    }

    public ExecutionStrategy createStrategy(ExternalProgram tsx, JSFixture testFixture, BaseFixturePool pool, TestRoot testRoot) {
        return constructor.call(tsx, testFixture, pool, testRoot);
    }
}
