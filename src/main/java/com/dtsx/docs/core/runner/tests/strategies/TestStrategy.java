package com.dtsx.docs.core.runner.tests.strategies;

import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.planner.meta.BaseMetaYml;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.lib.ExecutorUtils.DirectExecutor;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.core.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.core.runner.tests.results.TestRootResults;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public sealed abstract class TestStrategy<M extends BaseMetaYml> permits CompilesTestStrategy, SnapshotTestStrategy {
    protected final TestCtx ctx;
    protected final M meta;

    public abstract TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, Placeholders placeholders);

    protected final ExecutorService mkExecutor() {
        return (meta.parallel())
            ? Executors.newVirtualThreadPerTaskExecutor()
            : DirectExecutor.INSTANCE;
    }
}
