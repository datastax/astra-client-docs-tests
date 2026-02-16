package com.dtsx.docs.core.runner.tests.strategies.execution;

import com.dtsx.docs.core.planner.fixtures.BaseFixturePool;
import com.dtsx.docs.core.planner.fixtures.BaseFixturePool.FixtureIndex;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.results.TestOutcome;
import com.dtsx.docs.lib.CliLogger.MessageUpdater;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ExecutionStrategy {
    protected final ConcurrentHashMap<ClientLanguage, Map<Path, TestOutcome>> outcomes = new ConcurrentHashMap<>();

    public final Map<ClientLanguage, Map<Path, TestOutcome>> execute(Map<ClientLanguage, Set<Path>> testFiles, MessageUpdater msgUpdater, TestFileRunner testFileRunner) {
        executeImpl(testFiles, msgUpdater, testFileRunner);
        return outcomes;
    }

    protected abstract void executeImpl(Map<ClientLanguage, Set<Path>> testFiles, MessageUpdater msgUpdater, TestFileRunner testFileRunner);

    public interface TestFileRunner {
        TestOutcome run(BaseFixturePool pool, ClientLanguage language, Path path, FixtureIndex index);
    }
}
