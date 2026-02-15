package com.dtsx.docs.core.runner.tests.strategies.test;

import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.planner.meta.BaseMetaYml;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.core.runner.tests.strategies.execution.ExecutionStrategy;
import com.dtsx.docs.core.runner.tests.strategies.execution.ParallelExecutionStrategy;
import com.dtsx.docs.core.runner.tests.strategies.execution.SequentialExecutionStrategy;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.core.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.core.runner.tests.results.TestRootResults;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public sealed abstract class TestStrategy<M extends BaseMetaYml> permits CompilesTestStrategy, SnapshotTestStrategy {
    protected final TestCtx ctx;
    protected final M meta;

    public abstract TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, Placeholders placeholders);

    protected final ExecutionStrategy executionStrategy() {
        return (meta.parallel())
            ? ParallelExecutionStrategy.INSTANCE
            : SequentialExecutionStrategy.INSTANCE;
    }
}
