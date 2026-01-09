package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.runner.drivers.ClientLanguage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestResults {
    private final Map<JSFixture, List<TestRootResults>> results = new HashMap<>();

    public void add(JSFixture baseFixture, TestRootResults result) {
        results.computeIfAbsent(baseFixture, _ -> new ArrayList<>()).add(result);
    }

    public Map<JSFixture, List<TestRootResults>> unwrap() {
        return results;
    }

    public sealed interface TestOutcome {
        default boolean passed() {
            return this instanceof Passed || this instanceof DryPassed;
        }

        enum Passed implements TestOutcome { INSTANCE }
        enum DryPassed implements TestOutcome { INSTANCE }
        record Failed(Error error) implements TestOutcome {}
        record Errored(Exception error) implements TestOutcome {}
    }

    public record TestRootResults(TestRoot testRoot, Map<ClientLanguage, TestOutcome> outcomes) {
        public int passedCount() {
            return (int) outcomes.values().stream().filter(TestOutcome::passed).count();
        }

        public boolean allPassed() {
            return passedCount() == outcomes.size();
        }
    }

    public int totalTests() {
        return results.values().stream().flatMap(List::stream).mapToInt(rs -> rs.outcomes().size()).sum();
    }

    public int passedTests() {
        return results.values().stream().flatMap(List::stream).mapToInt(TestRootResults::passedCount).sum();
    }

    public int failedTests() {
        return totalTests() - passedTests();
    }

    public boolean allPassed() {
        return failedTests() == 0;
    }
}
