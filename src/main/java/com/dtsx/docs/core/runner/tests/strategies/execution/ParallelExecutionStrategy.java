package com.dtsx.docs.core.runner.tests.strategies.execution;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public enum ParallelExecutionStrategy implements ExecutionStrategy {
    INSTANCE;

    @Override
    public ExecutorService mkExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public Resetter mkResetter(Runnable beforeEach, Runnable afterEach) {
        return NoopResetter.INSTANCE;
    }

    @RequiredArgsConstructor
    private enum NoopResetter implements Resetter {
        INSTANCE;

        @Override
        public void beforeEach() {
            // noop
        }

        @Override
        public void afterEach() {
            // noop
        }
    }
}
