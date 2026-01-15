package com.dtsx.docs.runner.tests.strategies;

import com.dtsx.docs.planner.TestRoot;
import com.dtsx.docs.runner.Placeholders;
import com.dtsx.docs.commands.test.TestCtx;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.runner.tests.results.TestRootResults;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public sealed abstract class TestStrategy permits CompilesTestStrategy, SnapshotTestStrategy {
    protected final TestCtx ctx;

    public abstract TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, Placeholders placeholders);
}
