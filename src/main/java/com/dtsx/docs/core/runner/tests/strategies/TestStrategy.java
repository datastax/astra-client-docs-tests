package com.dtsx.docs.core.runner.tests.strategies;

import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.core.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.core.runner.tests.results.TestRootResults;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public sealed abstract class TestStrategy permits CompilesTestStrategy, SnapshotTestStrategy {
    protected final TestCtx ctx;

    public abstract TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, Placeholders placeholders);
}
