package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.runner.drivers.ClientLanguage;

import java.nio.file.Path;
import java.util.*;

public class TestResults {
    private final Map<JSFixture, List<TestRootResults>> results = new HashMap<>();

    public void add(JSFixture baseFixture, TestRootResults result) {
        results.computeIfAbsent(baseFixture, _ -> new ArrayList<>()).add(result);
    }

    public Map<JSFixture, List<TestRootResults>> unwrap() {
        return results;
    }

    public sealed interface TestOutcome {
        default String name() {
            return this.getClass().getSimpleName();
        }

        default boolean passed() {
            return this instanceof Passed || this instanceof DryPassed;
        }

        enum Passed implements TestOutcome { INSTANCE }
        enum DryPassed implements TestOutcome { INSTANCE }
        enum Mismatch implements TestOutcome { INSTANCE }
        record Failed(Optional<Path> expected) implements TestOutcome {}
        record Errored(Exception error) implements TestOutcome {}
    }

    public record PathsAndOutcome(Set<Path> paths, TestOutcome outcome) {}

    public record TestRootResults(TestRoot testRoot, Map<ClientLanguage, PathsAndOutcome> outcomes) {
        public int passedTests() {
            return outcomes.values().stream().mapToInt(po -> po.outcome().passed() ? po.paths.size() : 0).sum();
        }

        public int totalTests() {
            return outcomes.values().stream().mapToInt(po -> po.paths().size()).sum();
        }

        public boolean allPassed() {
            return passedTests() == totalTests();
        }
    }

    public int totalTests() {
        return results.values().stream().flatMap(List::stream).mapToInt(TestRootResults::totalTests).sum();
    }

    public int passedTests() {
        return results.values().stream().flatMap(List::stream).mapToInt(TestRootResults::passedTests).sum();
    }

    public int failedTests() {
        return totalTests() - passedTests();
    }

    public boolean allPassed() {
        return failedTests() == 0;
    }
}
