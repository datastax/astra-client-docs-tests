package com.dtsx.docs.core.runner.tests.strategies.execution;

import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.planner.fixtures.BaseFixturePool;
import com.dtsx.docs.core.planner.fixtures.BaseFixturePool.FixtureIndex;
import com.dtsx.docs.core.planner.fixtures.FixtureMetadata;
import com.dtsx.docs.core.planner.fixtures.JSFixture;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.results.TestOutcome;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.CliLogger.MessageUpdater;
import com.dtsx.docs.lib.ExecutorUtils;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class SharedExecutionStrategy extends ExecutionStrategy {
    private final ExternalProgram tsx;
    private final JSFixture testFixture;
    private final BaseFixturePool pool;
    private final TestRoot testRoot;

    private final FixtureIndex index = FixtureIndex.ZERO;

    public static BaseFixturePool slicePool(BaseFixturePool baseFixturePool, TestRoot ignored) {
        return baseFixturePool.slice(0, 1);
    }

    @Override
    protected void executeImpl(Map<ClientLanguage, Set<Path>> testFiles, MessageUpdater ignored, TestFileRunner testFileRunner) {
        try (val executor = Executors.newVirtualThreadPerTaskExecutor()) {
            pool.beforeEach(tsx);
            testFixture.setup(tsx, md());
            testFixture.beforeEach(tsx, md(), null);

            val futures = ExecutorUtils.emptyFuturesList();

            testFiles.forEach((lang, files) -> {
                futures.add(executor.submit(() -> {
                    executeLanguage(lang, files, testFileRunner);
                }));
            });

            ExecutorUtils.awaitAll(futures);
        } finally {
            testFixture.afterEach(tsx, md(), null);
            testFixture.teardown(tsx, md());
            pool.afterEach(tsx);
        }
    }

    private void executeLanguage(ClientLanguage language, Set<Path> filesForLang, TestFileRunner testFileRunner) {
        try {
            val result = testFileRunner.run(language, filesForLang, md(), TestResetter.NOOP, (_) -> {});
            outcomes.put(language, filesForLang.stream().collect(toMap(p -> p, _ -> result)));
        } catch (Exception ex) {
            CliLogger.exception("Error running snapshot tests for language '%s' in test root '%s'".formatted(language, testRoot.rootName()));
            outcomes.put(language, filesForLang.stream().collect(toMap(p -> p, _ -> new TestOutcome.Errored(ex).alsoLog(testRoot, language))));
        }
    }

    private FixtureMetadata md() {
        return pool.meta(tsx, index);
    }
}
