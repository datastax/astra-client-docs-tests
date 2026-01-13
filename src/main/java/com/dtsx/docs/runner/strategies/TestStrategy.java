package com.dtsx.docs.runner.strategies;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.FixtureMetadata;
import com.dtsx.docs.config.VerifierCtx;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.runner.ExecutionEnvironment.ExecutionEnvironments;
import com.dtsx.docs.runner.TestResults.TestRootResults;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public sealed abstract class TestStrategy permits CompilesTestStrategy, SnapshotTestStrategy {
    protected final VerifierCtx ctx;

    public abstract TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, FixtureMetadata md);
}
