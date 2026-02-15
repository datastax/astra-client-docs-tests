package com.dtsx.docs.core.runner.tests.strategies.execution;

import java.util.concurrent.ExecutorService;

public sealed interface ExecutionMode permits SequentialExecutionMode, ParallelExecutionMode {
    ExecutorService executor();
    Resetter resetter(Runnable beforeEach, Runnable afterEach);

    interface Resetter {
        void beforeEach();
        void afterEach();
    }
}
