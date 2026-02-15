package com.dtsx.docs.core.runner.tests.strategies.execution;

import java.util.concurrent.ExecutorService;

public sealed interface ExecutionStrategy permits SequentialExecutionStrategy, ParallelExecutionStrategy {
    ExecutorService mkExecutor();
    Resetter mkResetter(Runnable beforeEach, Runnable afterEach);

    interface Resetter {
        void beforeEach();
        void afterEach();
    }
}
