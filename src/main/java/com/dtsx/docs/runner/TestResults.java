package com.dtsx.docs.runner;

import com.dtsx.docs.builder.TestRoot;
import com.dtsx.docs.builder.fixtures.JSFixture;
import com.dtsx.docs.runner.drivers.ClientLanguage;
import picocli.CommandLine.Help.Ansi.Style;

import java.util.*;

import static com.dtsx.docs.lib.ColorUtils.color;

public class TestResults {
    private final Map<JSFixture, List<TestRootResults>> results = new HashMap<>();

    public void add(JSFixture baseFixture, TestRootResults result) {
        results.computeIfAbsent(baseFixture, _ -> new ArrayList<>()).add(result);
    }

    public Map<JSFixture, List<TestRootResults>> unwrap() {
        return results;
    }

    public sealed interface TestOutcome {
        String status();

        enum Passed implements TestOutcome {
            INSTANCE;

            @Override
            public String status() {
                return color(Style.fg_green, "(PASSED)");
            }
        }

        record Failed(Error error) implements TestOutcome {
            @Override
            public String status() {
                return color(Style.fg_red, "(FAILED)");
            }
        }

        record Errored(Exception error) implements TestOutcome {
            @Override
            public String status() {
                return color(Style.fg_yellow, "(ERRORED)");
            }
        }
    }

    public record TestRootResults(TestRoot testRoot, Map<ClientLanguage, TestOutcome> outcomes) {
        public boolean approved(ClientLanguage language) {
            return outcomes.get(language) instanceof TestOutcome.Passed;
        }

        public int approvedCount() {
            return (int) outcomes.keySet().stream().filter(this::approved).count();
        }

        public boolean allApproved() {
            return approvedCount() == outcomes.size();
        }
    }

    public int totalTests() {
        return results.values().stream().flatMap(List::stream).mapToInt(rs -> rs.outcomes().size()).sum();
    }

    public int passedTests() {
        return results.values().stream().flatMap(List::stream).mapToInt(TestRootResults::approvedCount).sum();
    }

    public int failedTests() {
        return totalTests() - passedTests();
    }

    public boolean allApproved() {
        return failedTests() == 0;
    }
}
