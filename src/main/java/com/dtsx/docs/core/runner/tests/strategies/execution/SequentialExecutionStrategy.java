package com.dtsx.docs.core.runner.tests.strategies.execution;

import com.dtsx.docs.lib.ExecutorUtils.DirectExecutor;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ExecutorService;

public enum SequentialExecutionStrategy implements ExecutionStrategy {
    INSTANCE;

    @Override
    public ExecutorService mkExecutor() {
        return DirectExecutor.INSTANCE;
    }

    @Override
    public Resetter mkResetter(Runnable beforeEach, Runnable afterEach) {
        return new BasicResetter(beforeEach, afterEach);
    }

    @RequiredArgsConstructor
    private static class BasicResetter implements Resetter {
        private final Runnable beforeEach;
        private final Runnable afterEach;

        @Override
        public void beforeEach() {
            beforeEach.run();
        }

        @Override
        public void afterEach() {
            afterEach.run();
        }
    }
}
