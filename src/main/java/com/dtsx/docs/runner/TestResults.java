package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import lombok.val;

import java.nio.file.Path;
import java.util.*;

import static com.dtsx.docs.lib.CliLogger.captureStackTrace;

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

        enum Mismatch implements TestOutcome {
            INSTANCE;

            public Mismatch alsoLog(TestRoot testRoot, ClientLanguage language, Map<String, Set<Path>> differingSnapshots) {
                val extra = new StringBuilder("Differing snapshots found for the following files:\n");

                for (val entry : differingSnapshots.entrySet()) {
                    extra.append("Snapshot:\n").append(entry.getKey()).append("\nFiles:\n");

                    for (val path : entry.getValue()) {
                        extra.append(" - ").append(path).append("\n");
                    }
                }

                CliLogger.result(testRoot, language, this, extra.toString());
                return this;
            }
        }

        record FailedToVerify(Optional<Path> expected) implements TestOutcome {
            public FailedToVerify alsoLog(TestRoot testRoot, ClientLanguage language, String actualSnapshot) {
                val prefix = expected
                    .map((path) -> "Expected snapshot file: " + path)
                    .orElse("No approved snapshot file found.");

                val extra = prefix + "\nActual snapshot:\n" + actualSnapshot;

                CliLogger.result(testRoot, language, this, extra);
                return this;
            }
        }

        record FailedToCompile(String message) implements TestOutcome {
            public FailedToCompile alsoLog(TestRoot testRoot, ClientLanguage language, String output) {
                CliLogger.result(testRoot, language, this, "Compilation output:\n" + output);
                return this;
            }
        }

        record Errored(Exception error) implements TestOutcome {  // TODO consider actually using this somewhere
            public Errored alsoLog(TestRoot testRoot, ClientLanguage language) {
                CliLogger.result(testRoot, language, this, captureStackTrace(error));
                return this;
            }
        }
    }

    public record TestRootResults(TestRoot testRoot, Map<ClientLanguage, Map<Path, TestOutcome>> outcomes) {
        public int passedTests() {
            return outcomes.values().stream().mapToInt(langMap ->
                (int) langMap.values().stream().filter(TestOutcome::passed).count()
            ).sum();
        }

        public int totalTests() {
            return outcomes.values().stream().mapToInt(Map::size).sum();
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
